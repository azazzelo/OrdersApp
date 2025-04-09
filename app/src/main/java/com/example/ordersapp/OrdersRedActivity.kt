package com.example.ordersapp

import android.app.Activity
import android.app.DatePickerDialog
// --- НУЖНЫЕ ИМПОРТЫ ДЛЯ ПЕЧАТИ ---
import android.content.Context
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
// ------------------------------------
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.withTransaction
import com.example.ordersapp.adapters.OrderProductAdapter
import com.example.ordersapp.adapters.ProductInOrderDisplay
import com.example.ordersapp.databinding.ActivityOrdersRedBinding
import com.example.ordersapp.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File // Все еще нужен для AvailabilityCheckResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrdersRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersRedBinding
    private lateinit var database: AppDatabase
    private lateinit var ordersDao: OrdersDao
    private lateinit var ordersProductsDao: OrdersProductsDao
    private lateinit var productsDao: ProductsDao
    private lateinit var customersDao: CustomersDao

    private lateinit var orderProductAdapter: OrderProductAdapter

    private var currentOrder: OrdersEntity? = null
    private var currentProductsInOrder = mutableListOf<ProductInOrderDisplay>()

    private var registrationDate = Calendar.getInstance()
    private var deliveryDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private var customersList = listOf<CustomersEntity>()
    private var selectedCustomerId: Int? = null

    // Ссылка на WebView для печати (чтобы избежать утечек)
    private var webViewForPrinting: WebView? = null

    private val addProductResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val productId = result.data?.getIntExtra("selected_product_id", -1) ?: -1
            val quantity = result.data?.getIntExtra("selected_quantity", 0) ?: 0
            Log.d("OrdersRedActivity", "Received product: ID=$productId, Quantity=$quantity")
            if (productId != -1 && quantity > 0) {
                addProductToOrder(productId, quantity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        ordersDao = database.ordersDao()
        ordersProductsDao = database.ordersProductsDao()
        productsDao = database.productsDao()
        customersDao = database.customersDao()

        setupRecyclerView()
        setupDatePickers()

        binding.edTextCustomerDisplay.setOnClickListener { showCustomerSelectionDialog() }
        binding.btnSelectCustomer.setOnClickListener { showCustomerSelectionDialog() }

        val orderId = intent.getIntExtra("order_id", -1)
        if (orderId != -1) {
            loadOrderData(orderId)
        } else {
            updateDateEditTexts()
            calculateAndDisplayTotalCost()
            binding.tvOrderId.text = "New Order"
            binding.edTextCustomerDisplay.hint = "Customer not selected"
        }

        binding.btnAddProduct.setOnClickListener {
            val intent = Intent(this, AddProductInOrderActivity::class.java)
            val existingProductIds = currentProductsInOrder.map { it.product.idProduct }.toIntArray()
            intent.putExtra("existing_product_ids", existingProductIds)
            addProductResultLauncher.launch(intent)
        }

        binding.btnSaveOrder.setOnClickListener {
            validateAndSaveOrder()
        }
    }

    override fun onDestroy() {
        // Очищаем WebView, чтобы избежать утечек памяти
        webViewForPrinting?.destroy()
        webViewForPrinting = null
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        orderProductAdapter = OrderProductAdapter(
            onProductDelete = { productInOrder ->
                AlertDialog.Builder(this)
                    .setTitle("Remove Product?")
                    .setMessage("Remove '${productInOrder.product.name}' (${productInOrder.quantityInOrder} units) from the order?")
                    .setPositiveButton("Remove") { _, _ ->
                        currentProductsInOrder.remove(productInOrder)
                        orderProductAdapter.removeItem(productInOrder)
                        calculateAndDisplayTotalCost()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onQuantityChange = { _, _ -> /* Not implemented */ }
        )
        binding.rcProductsInOrder.adapter = orderProductAdapter
        binding.rcProductsInOrder.layoutManager = LinearLayoutManager(this)
    }

    private fun setupDatePickers() {
        val regDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            registrationDate.set(Calendar.YEAR, year)
            registrationDate.set(Calendar.MONTH, monthOfYear)
            registrationDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateEditTexts()
        }
        binding.edTextRegDate.isFocusable = false
        binding.edTextRegDate.setOnClickListener {
            DatePickerDialog(this, regDateSetListener,
                registrationDate.get(Calendar.YEAR),
                registrationDate.get(Calendar.MONTH),
                registrationDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        val delDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            deliveryDate.set(Calendar.YEAR, year)
            deliveryDate.set(Calendar.MONTH, monthOfYear)
            deliveryDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateEditTexts()
        }
        binding.edTextDelDate.isFocusable = false
        binding.edTextDelDate.setOnClickListener {
            DatePickerDialog(this, delDateSetListener,
                deliveryDate.get(Calendar.YEAR),
                deliveryDate.get(Calendar.MONTH),
                deliveryDate.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDateEditTexts() {
        binding.edTextRegDate.setText(dateFormat.format(registrationDate.time))
        binding.edTextDelDate.setText(dateFormat.format(deliveryDate.time))
    }

    private fun showCustomerSelectionDialog() {
        lifecycleScope.launch {
            try {
                customersList = customersDao.getAllCustomers()
                val customerNames = customersList.map { it.name }.toTypedArray()

                if (customerNames.isEmpty()) {
                    Toast.makeText(this@OrdersRedActivity, "No customers available", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                AlertDialog.Builder(this@OrdersRedActivity)
                    .setTitle("Select Customer")
                    .setItems(customerNames) { _, which ->
                        val selectedCustomer = customersList[which]
                        selectedCustomerId = selectedCustomer.idCustomer
                        binding.edTextCustomerDisplay.setText(selectedCustomer.name)
                        binding.edTextCustomerDisplay.error = null
                        Log.d("OrdersRedActivity", "Customer selected via dialog: ID=$selectedCustomerId, Name=${selectedCustomer.name}")
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                Log.e("OrdersRedActivity", "Error showing customer selection dialog", e)
                Toast.makeText(this@OrdersRedActivity, "Error loading customers", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun loadOrderData(orderId: Int) {
        lifecycleScope.launch {
            try {
                currentOrder = ordersDao.getOrderById(orderId)
                if (currentOrder == null) {
                    Toast.makeText(this@OrdersRedActivity, "Order not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                binding.tvOrderId.text = "Order #${currentOrder!!.idOrder}"
                registrationDate.timeInMillis = currentOrder!!.dateRegistration
                deliveryDate.timeInMillis = currentOrder!!.dateDelivery
                updateDateEditTexts()

                selectedCustomerId = currentOrder!!.idCustomer
                val customer = withContext(Dispatchers.IO){ customersDao.getCustomerById(selectedCustomerId!!) }
                if(customer != null){
                    binding.edTextCustomerDisplay.setText(customer.name)
                    binding.edTextCustomerDisplay.error = null
                } else {
                    binding.edTextCustomerDisplay.setText("Customer ID: $selectedCustomerId (not found)")
                    selectedCustomerId = null
                }

                val orderProductsEntities = ordersProductsDao.getProductsForOrder(orderId)
                val productDetailsList = mutableListOf<ProductInOrderDisplay>()
                for (opEntity in orderProductsEntities) {
                    val productEntity = withContext(Dispatchers.IO) { productsDao.getProductById(opEntity.idProduct) }
                    if (productEntity != null) {
                        productDetailsList.add(ProductInOrderDisplay(productEntity, opEntity.quantity))
                    } else {
                        Log.w("OrdersRedActivity", "Product with ID ${opEntity.idProduct} not found for order $orderId")
                    }
                }
                currentProductsInOrder = productDetailsList
                orderProductAdapter.submitList(currentProductsInOrder)
                calculateAndDisplayTotalCost()

            } catch (e: Exception) {
                Log.e("OrdersRedActivity", "Error loading order data for ID $orderId", e)
                Toast.makeText(this@OrdersRedActivity, "Error loading order data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun addProductToOrder(productId: Int, quantity: Int) {
        lifecycleScope.launch {
            try {
                val product = productsDao.getProductById(productId)
                if (product != null) {
                    val existingItem = currentProductsInOrder.find { it.product.idProduct == productId }
                    val currentQuantityInOrder = existingItem?.quantityInOrder ?: 0
                    val requiredTotalQuantity = currentQuantityInOrder + quantity

                    val productInDb = withContext(Dispatchers.IO) { productsDao.getProductById(productId) }
                    val availableStock = productInDb?.quantity ?: 0

                    if (availableStock >= quantity ) {
                        if(existingItem != null) {
                            if (availableStock >= requiredTotalQuantity) {
                                existingItem.quantityInOrder = requiredTotalQuantity
                                orderProductAdapter.notifyItemChanged(currentProductsInOrder.indexOf(existingItem))
                                calculateAndDisplayTotalCost()
                                Log.d("OrdersRedActivity", "Updated product ${product.name} (Total Qty: $requiredTotalQuantity) in local list.")
                            } else {
                                Toast.makeText(this@OrdersRedActivity, "Insufficient stock for '${product.name}' (${availableStock} units) to increase quantity", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val newItem = ProductInOrderDisplay(product, quantity)
                            currentProductsInOrder.add(newItem)
                            orderProductAdapter.addItem(newItem)
                            calculateAndDisplayTotalCost()
                            Log.d("OrdersRedActivity", "Added product ${product.name} (Qty: $quantity) to local list.")
                        }
                    } else {
                        Toast.makeText(this@OrdersRedActivity, "Insufficient stock for '${product.name}' (${availableStock} units) to add $quantity units", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@OrdersRedActivity, "Product with ID $productId not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OrdersRedActivity", "Error adding product $productId to order", e)
                Toast.makeText(this@OrdersRedActivity, "Error adding product", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateAndDisplayTotalCost() {
        val totalCost = currentProductsInOrder.sumOf { it.product.cost * it.quantityInOrder }
        binding.tvFinallyCost.text = String.format(Locale.US, "Total: $%.2f", totalCost)
    }

    private fun validateAndSaveOrder() {
        Log.d("OrdersRedActivity", "Validating order before save.")
        // Validation
        if (selectedCustomerId == null) {
            binding.edTextCustomerDisplay.error = "Customer must be selected"
            Toast.makeText(this, "Customer not selected", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.edTextCustomerDisplay.error = null
        }
        if (currentProductsInOrder.isEmpty()) {
            Toast.makeText(this, "Add at least one product to the order", Toast.LENGTH_SHORT).show()
            return
        }
        if (deliveryDate.timeInMillis < registrationDate.timeInMillis) {
            binding.edTextDelDate.error = "Delivery date cannot be earlier than registration date"
            Toast.makeText(this, "Invalid delivery date", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.edTextDelDate.error = null
        }

        // Preliminary check for product availability
        lifecycleScope.launch {
            val availabilityCheckResult = checkProductAvailability()
            if (!availabilityCheckResult.isAvailable) {
                Log.w("OrdersRedActivity", "Not enough stock for products: ${availabilityCheckResult.unavailableMessages.joinToString()}")
                AlertDialog.Builder(this@OrdersRedActivity)
                    .setTitle("Insufficient Stock")
                    .setMessage("The following products have insufficient stock:\n${availabilityCheckResult.unavailableMessages.joinToString("\n")}")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            // If all available, proceed with saving
            Log.d("OrdersRedActivity", "All products available. Proceeding with save.")
            val finalCost = currentProductsInOrder.sumOf { it.product.cost * it.quantityInOrder }
            val regTimestamp = registrationDate.timeInMillis
            val delTimestamp = deliveryDate.timeInMillis
            val customerId = selectedCustomerId!!

            val orderEntity = currentOrder?.copy(
                idCustomer = customerId,
                dateRegistration = regTimestamp,
                dateDelivery = delTimestamp,
                finallyCost = finalCost
            ) ?: OrdersEntity(
                idCustomer = customerId,
                dateRegistration = regTimestamp,
                dateDelivery = delTimestamp,
                finallyCost = finalCost
            )

            val productsSnapshot = currentProductsInOrder.toList() // Use snapshot
            val orderProductEntitiesToSave = productsSnapshot.map {
                OrdersProductsEntity(0, it.product.idProduct, it.quantityInOrder)
            }
            val existingOrderId = currentOrder?.idOrder

            // Launch coroutine for the save transaction
            launch {
                var savedOrderId: Int? = null
                var success = false
                var customerData: CustomersEntity? = null // For PDF

                try {
                    savedOrderId = executeSaveOrderTransaction(orderEntity, orderProductEntitiesToSave, existingOrderId)
                    // Load customer data AFTER successful save for PDF
                    customerData = withContext(Dispatchers.IO) {
                        customersDao.getCustomerById(customerId)
                    }
                    success = true

                } catch (e: Exception) {
                    Log.e("OrdersRedActivity", "Error during save order transaction", e)
                    Toast.makeText(this@OrdersRedActivity, "Error saving order: ${e.message}", Toast.LENGTH_LONG).show()
                }

                // --- If save was successful, generate HTML and initiate PRINT ---
                if (success && savedOrderId != null) {
                    Log.i("OrdersRedActivity", "Order save transaction successful for order ID: $savedOrderId")
                    Toast.makeText(this@OrdersRedActivity, "Order saved. Preparing document...", Toast.LENGTH_SHORT).show() // English
                    setResult(Activity.RESULT_OK)

                    // Create HTML content
                    val savedOrderEntity = orderEntity.copy(idOrder = savedOrderId)
                    val htmlContent = generateOrderHtml(savedOrderEntity, customerData, productsSnapshot)

                    // Initiate printing (which allows saving as PDF)
                    createPdfFromHtml(savedOrderId, htmlContent)

                    // IMPORTANT: Don't call finish() here. The print dialog will appear.
                    // finish() // <-- REMOVE this finish() call

                }
                // If there was an error, the activity doesn't close
            }
        }
    }

    // Data class for AvailabilityCheckResult
    private data class AvailabilityCheckResult(val isAvailable: Boolean, val unavailableMessages: List<String>)

    // Suspend function to check product availability
    private suspend fun checkProductAvailability(): AvailabilityCheckResult {
        var allProductsAvailable = true
        val unavailableMessages = mutableListOf<String>()
        val existingOrderId = currentOrder?.idOrder

        withContext(Dispatchers.IO) {
            val oldOrderProductsMap = if(existingOrderId != null) {
                ordersProductsDao.getProductsForOrder(existingOrderId).associateBy { it.idProduct }
            } else {
                emptyMap()
            }

            for (item in currentProductsInOrder) {
                val productInDb = productsDao.getProductById(item.product.idProduct)
                val currentDbQuantity = productInDb?.quantity ?: 0
                val quantityAlreadyInThisOrder = oldOrderProductsMap[item.product.idProduct]?.quantity ?: 0
                val availableQuantity = currentDbQuantity + quantityAlreadyInThisOrder

                if (availableQuantity < item.quantityInOrder) {
                    allProductsAvailable = false
                    unavailableMessages.add("${item.product.name} (needed ${item.quantityInOrder}, available $availableQuantity)")
                }
            }
        }
        return AvailabilityCheckResult(allProductsAvailable, unavailableMessages)
    }


    // Suspend function for the database transaction
    private suspend fun executeSaveOrderTransaction(
        orderToSave: OrdersEntity,
        productsToSave: List<OrdersProductsEntity>,
        existingOrderId: Int?
    ): Int {
        var resultingOrderId: Int = -1

        database.withTransaction {
            var orderIdToUse: Int
            var oldOrderProducts: List<OrdersProductsEntity> = emptyList()

            if (existingOrderId == null) {
                val newOrderId = ordersDao.insertOrder(orderToSave)
                orderIdToUse = newOrderId.toInt()
                Log.i("OrdersRedActivity", "TX: New order inserted with ID: $orderIdToUse")
            } else {
                orderIdToUse = existingOrderId
                oldOrderProducts = ordersProductsDao.getProductsForOrder(orderIdToUse)
                ordersDao.updateOrder(orderToSave.copy(idOrder = orderIdToUse))
                ordersProductsDao.deleteProductsForOrder(orderIdToUse)
                Log.i("OrdersRedActivity", "TX: Existing order $orderIdToUse updated. Old links deleted.")
            }
            resultingOrderId = orderIdToUse

            val finalOrderProductEntities = productsToSave.map { it.copy(idOrder = orderIdToUse) }

            for (oldItem in oldOrderProducts) {
                productsDao.increaseProductQuantity(oldItem.idProduct, oldItem.quantity)
                Log.d("OrdersRedActivity", "TX: Returned ${oldItem.quantity} of product ${oldItem.idProduct} to stock.")
            }

            ordersProductsDao.insertOrderProducts(finalOrderProductEntities)
            Log.d("OrdersRedActivity", "TX: Inserted ${finalOrderProductEntities.size} new product links for order $orderIdToUse")

            for (newItem in finalOrderProductEntities) {
                val updatedRows = productsDao.decreaseProductQuantity(newItem.idProduct, newItem.quantity)
                if (updatedRows == 0) {
                    throw Exception("TX: Failed to decrease quantity for product ${newItem.idProduct}.")
                }
                Log.d("OrdersRedActivity", "TX: Decreased stock for product ${newItem.idProduct} by ${newItem.quantity}.")
            }
        }
        return resultingOrderId
    }


    // --- МЕТОДЫ ДЛЯ ГЕНЕРАЦИИ HTML И ПЕЧАТИ/СОХРАНЕНИЯ PDF ---

    // Вспомогательная функция для генерации HTML-строки для заказа
    private fun generateOrderHtml(
        order: OrdersEntity,
        customer: CustomersEntity?,
        products: List<ProductInOrderDisplay>
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) // Формат даты
        val priceFormat = "%.2f" // Формат цены

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<!DOCTYPE html><html><head>")
        htmlBuilder.append("<meta charset=\"UTF-8\">")
        htmlBuilder.append("""
            <style>
                body { font-family: sans-serif; padding: 15px; }
                h1 { text-align: center; color: #D32F2F; } /* Red color */
                h2 { color: #555; margin-top: 20px; border-bottom: 1px solid #ccc; padding-bottom: 5px; }
                table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 10pt; } /* Smaller font */
                th { background-color: #f2f2f2; font-weight: bold; }
                .text-right { text-align: right; }
                .total-row td { font-weight: bold; border-top: 2px solid #555; }
                .customer-details p, .order-details p { margin: 4px 0; font-size: 11pt; }
            </style>
        """.trimIndent())
        htmlBuilder.append("</head><body>")
        htmlBuilder.append("<h1>Order #${order.idOrder}</h1>")

        // --- Customer Details ---
        htmlBuilder.append("<div class='customer-details'>")
        htmlBuilder.append("<h2>Customer Details</h2>")
        if (customer != null) {
            htmlBuilder.append("<p><b>Name:</b> ${customer.name ?: ""}</p>")
            customer.address?.let { htmlBuilder.append("<p><b>Address:</b> $it</p>") }
            htmlBuilder.append("<p><b>Phone:</b> ${customer.phone ?: ""}</p>")
            htmlBuilder.append("<p><b>Email:</b> ${customer.contactPersonEmail ?: ""}</p>")
        } else {
            htmlBuilder.append("<p>Customer (ID: ${order.idCustomer}) not found or deleted.</p>")
        }
        htmlBuilder.append("</div>")

        // --- Order Details ---
        htmlBuilder.append("<div class='order-details'>")
        htmlBuilder.append("<h2>Order Details</h2>")
        htmlBuilder.append("<p><b>Registration Date:</b> ${dateFormat.format(Date(order.dateRegistration))}</p>")
        htmlBuilder.append("<p><b>Delivery Date:</b> ${dateFormat.format(Date(order.dateDelivery))}</p>")
        htmlBuilder.append("</div>")

        // --- Products Table ---
        htmlBuilder.append("<h2>Products</h2>")
        htmlBuilder.append("<table>")
        htmlBuilder.append("<thead><tr>")
        htmlBuilder.append("<th>Product Name</th>")
        htmlBuilder.append("<th class='text-right'>Unit Price</th>")
        htmlBuilder.append("<th class='text-right'>Quantity</th>")
        htmlBuilder.append("<th class='text-right'>Total</th>")
        htmlBuilder.append("</tr></thead>")
        htmlBuilder.append("<tbody>")
        products.forEach { item ->
            val itemTotal = item.product.cost * item.quantityInOrder
            htmlBuilder.append("<tr>")
            htmlBuilder.append("<td>${item.product.name ?: "N/A"}</td>")
            htmlBuilder.append("<td class='text-right'>${String.format(Locale.US, priceFormat, item.product.cost)}</td>")
            htmlBuilder.append("<td class='text-right'>${item.quantityInOrder}</td>")
            htmlBuilder.append("<td class='text-right'>${String.format(Locale.US, priceFormat, itemTotal)}</td>")
            htmlBuilder.append("</tr>")
        }
        htmlBuilder.append("</tbody>")
        // --- Total Row ---
        htmlBuilder.append("<tr class='total-row'>")
        htmlBuilder.append("<td colspan='3' class='text-right'><b>Final Cost:</b></td>")
        htmlBuilder.append("<td class='text-right'><b>${String.format(Locale.US, priceFormat, order.finallyCost)}</b></td>")
        htmlBuilder.append("</tr>")
        htmlBuilder.append("</table>")

        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }


    // Функция для вызова печати/сохранения PDF через WebView
    private fun createPdfFromHtml(orderId: Int, htmlContent: String) {
        Log.d("OrdersRedActivity", "Creating WebView for PDF generation (Order ID: $orderId)")
        // Создаем WebView программно
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            // Важно для правильного рендеринга перед печатью
            settings.layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    Log.d("OrdersRedActivity", "WebView finished loading HTML content for Order ID $orderId.")
                    // Сохраняем ссылку на WebView, чтобы потом можно было уничтожить
                    webViewForPrinting = view
                    // Запускаем печать/сохранение
                    initiatePrintJob(view, orderId)
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e("OrdersRedActivity", "WebView error loading HTML: $errorCode - $description")
                    Toast.makeText(this@OrdersRedActivity, "Error loading order data for PDF.", Toast.LENGTH_SHORT).show()
                    // Если загрузка HTML не удалась, просто закрываем активность
                    if (!isFinishing && !isDestroyed) { finish() }
                }
            }
        }
        // Загружаем HTML
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    // Инициирует системный диалог печати
    private fun initiatePrintJob(webView: WebView, orderId: Int) {
        // Получаем PrintManager
        val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Log.e("OrdersRedActivity", "PrintManager service not available.")
            Toast.makeText(this, "Printing service not available.", Toast.LENGTH_SHORT).show()
            if (!isFinishing && !isDestroyed) { finish() } // Закрываем, если сервис недоступен
            return
        }

        val jobName = "Order_$orderId" // Имя файла по умолчанию

        try {
            // Создаем адаптер печати
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            // Атрибуты печати (A4 по умолчанию)
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            // Запускаем задание на печать
            printManager.print(jobName, printAdapter, printAttributes)
            Log.i("OrdersRedActivity", "Print job initiated for $jobName")
            // Важно: НЕ ВЫЗЫВАЕМ finish() здесь.
            // Активность закроется после того, как пользователь
            // завершит работу с системным диалогом печати/сохранения.

        } catch (e: Exception) {
            Log.e("OrdersRedActivity", "Error initiating print job for $jobName", e)
            Toast.makeText(this, "Failed to initiate PDF creation.", Toast.LENGTH_SHORT).show()
            if (!isFinishing && !isDestroyed) { finish() } // Закрываем при ошибке инициации печати
        }
    }

} // Конец класса OrdersRedActivity