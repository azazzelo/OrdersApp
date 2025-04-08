package com.example.ordersapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
// Убрали импорт AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.withTransaction // Убедитесь, что импорт есть
import com.example.ordersapp.adapters.OrderProductAdapter // Проверьте путь
import com.example.ordersapp.adapters.ProductInOrderDisplay // Проверьте путь
import com.example.ordersapp.databinding.ActivityOrdersRedBinding
import com.example.ordersapp.db.* // Импортируем всё из db для DAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Список клиентов для диалога - загружается при нажатии кнопки
    private var customersList = listOf<CustomersEntity>()
    // Храним только ID выбранного клиента
    private var selectedCustomerId: Int? = null

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
        // Убрали loadCustomersForSpinner() отсюда

        // Добавляем слушатели для выбора клиента
        binding.edTextCustomerDisplay.setOnClickListener { showCustomerSelectionDialog() }
        binding.btnSelectCustomer.setOnClickListener { showCustomerSelectionDialog() }

        val orderId = intent.getIntExtra("order_id", -1)
        if (orderId != -1) {
            Log.d("OrdersRedActivity", "Editing order with ID: $orderId")
            loadOrderData(orderId)
        } else {
            Log.d("OrdersRedActivity", "Creating new order.")
            updateDateEditTexts()
            calculateAndDisplayTotalCost()
            binding.tvOrderId.text = "Новый заказ"
            binding.edTextCustomerDisplay.hint = "Клиент не выбран" // Устанавливаем подсказку
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

    private fun setupRecyclerView() {
        orderProductAdapter = OrderProductAdapter(
            onProductDelete = { productInOrder ->
                // Диалог подтверждения уже вызывается в адаптере
                currentProductsInOrder.remove(productInOrder)
                orderProductAdapter.removeItem(productInOrder)
                calculateAndDisplayTotalCost()
            },
            onQuantityChange = { _, _ -> /* Пока не реализовано */ }
        )
        // Убедитесь, что ID вашего RecyclerView в XML - rcProductsInOrder
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

    // Новый метод для показа диалога выбора клиента
    private fun showCustomerSelectionDialog() {
        lifecycleScope.launch {
            try {
                customersList = customersDao.getAllCustomers() // Загружаем актуальный список
                val customerNames = customersList.map { it.name }.toTypedArray() // Имена для диалога

                if (customerNames.isEmpty()) {
                    Toast.makeText(this@OrdersRedActivity, "Нет доступных клиентов для выбора", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                AlertDialog.Builder(this@OrdersRedActivity)
                    .setTitle("Выберите клиента")
                    .setItems(customerNames) { dialog, which ->
                        // 'which' - это индекс выбранного элемента
                        val selectedCustomer = customersList[which]
                        selectedCustomerId = selectedCustomer.idCustomer // Сохраняем ID
                        binding.edTextCustomerDisplay.setText(selectedCustomer.name) // Обновляем поле отображения
                        binding.edTextCustomerDisplay.error = null // Сбрасываем ошибку, если была
                        Log.d("OrdersRedActivity", "Customer selected via dialog: ID=$selectedCustomerId, Name=${selectedCustomer.name}")
                    }
                    .setNegativeButton("Отмена", null)
                    .show()

            } catch (e: Exception) {
                Log.e("OrdersRedActivity", "Error showing customer selection dialog", e)
                Toast.makeText(this@OrdersRedActivity, "Ошибка загрузки клиентов", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun loadOrderData(orderId: Int) {
        lifecycleScope.launch {
            try {
                currentOrder = ordersDao.getOrderById(orderId)
                if (currentOrder == null) {
                    Toast.makeText(this@OrdersRedActivity, "Заказ не найден", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                binding.tvOrderId.text = "Заказ №${currentOrder!!.idOrder}"
                registrationDate.timeInMillis = currentOrder!!.dateRegistration
                deliveryDate.timeInMillis = currentOrder!!.dateDelivery
                updateDateEditTexts()

                // Загружаем имя клиента для отображения
                selectedCustomerId = currentOrder!!.idCustomer // Сохраняем ID
                val customer = withContext(Dispatchers.IO){ customersDao.getCustomerById(selectedCustomerId!!) }
                if(customer != null){
                    binding.edTextCustomerDisplay.setText(customer.name)
                    binding.edTextCustomerDisplay.error = null
                } else {
                    binding.edTextCustomerDisplay.setText("Клиент ID: $selectedCustomerId (не найден)")
                    selectedCustomerId = null // Сбрасываем ID, т.к. клиент удален
                }

                // Загружаем товары
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
                Toast.makeText(this@OrdersRedActivity, "Ошибка загрузки данных заказа", Toast.LENGTH_SHORT).show()
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

                    // Проверяем доступное количество на складе
                    val productInDb = withContext(Dispatchers.IO) { productsDao.getProductById(productId) }
                    val availableStock = productInDb?.quantity ?: 0

                    if (availableStock >= quantity ) { // Достаточно для добавления *запрошенного* количества
                        if(existingItem != null) {
                            // Проверяем, не превысит ли новое общее количество остаток на складе
                            if (availableStock >= requiredTotalQuantity) {
                                existingItem.quantityInOrder = requiredTotalQuantity
                                orderProductAdapter.notifyItemChanged(currentProductsInOrder.indexOf(existingItem))
                                calculateAndDisplayTotalCost()
                                Log.d("OrdersRedActivity", "Updated product ${product.name} (Total Qty: $requiredTotalQuantity) in local list.")
                            } else {
                                Toast.makeText(this@OrdersRedActivity, "Недостаточно товара '${product.name}' на складе (${availableStock} шт.) для увеличения количества", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val newItem = ProductInOrderDisplay(product, quantity)
                            currentProductsInOrder.add(newItem)
                            orderProductAdapter.addItem(newItem) // addItem сам обработает дубликат, если нужно
                            calculateAndDisplayTotalCost()
                            Log.d("OrdersRedActivity", "Added product ${product.name} (Qty: $quantity) to local list.")
                        }
                    } else {
                        Toast.makeText(this@OrdersRedActivity, "Недостаточно товара '${product.name}' на складе (${availableStock} шт.) для добавления $quantity шт.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@OrdersRedActivity, "Товар с ID $productId не найден", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OrdersRedActivity", "Error adding product $productId to order", e)
                Toast.makeText(this@OrdersRedActivity, "Ошибка добавления товара", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateAndDisplayTotalCost() {
        val totalCost = currentProductsInOrder.sumOf { it.product.cost * it.quantityInOrder }
        binding.tvFinallyCost.text = String.format(Locale.ROOT, "Итого: %.2f", totalCost)
    }

    private fun validateAndSaveOrder() {
        Log.d("OrdersRedActivity", "Validating order before save.")
        // --- Валидация ---
        if (selectedCustomerId == null) {
            // Используем поле отображения для ошибки
            binding.edTextCustomerDisplay.error = "Клиент должен быть выбран"
            Toast.makeText(this, "Клиент не выбран", Toast.LENGTH_SHORT).show()
            return
        } else {
            // Сбрасываем ошибку, если клиент выбран
            binding.edTextCustomerDisplay.error = null
        }
        if (currentProductsInOrder.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы один товар в заказ", Toast.LENGTH_SHORT).show()
            return
        }
        if (deliveryDate.timeInMillis < registrationDate.timeInMillis) {
            binding.edTextDelDate.error = "Дата доставки не может быть раньше даты регистрации"
            Toast.makeText(this, "Некорректная дата доставки", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.edTextDelDate.error = null // Сбрасываем ошибку
        }

        // --- Предварительная проверка наличия товаров ---
        lifecycleScope.launch {
            val availabilityCheckResult = checkProductAvailability()
            if (!availabilityCheckResult.isAvailable) {
                Log.w("OrdersRedActivity", "Not enough stock for products: ${availabilityCheckResult.unavailableMessages.joinToString()}")
                AlertDialog.Builder(this@OrdersRedActivity)
                    .setTitle("Недостаточно товара")
                    .setMessage("Не хватает следующих товаров на складе:\n${availabilityCheckResult.unavailableMessages.joinToString("\n")}")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            // --- Если все доступно, запускаем сохранение ---
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

            val currentProductsSnapshot = currentProductsInOrder.map {
                OrdersProductsEntity(0, it.product.idProduct, it.quantityInOrder)
            }
            val existingOrderId = currentOrder?.idOrder

            // Запускаем корутину для выполнения самой транзакции
            launch {
                try {
                    val savedOrderId = executeSaveOrderTransaction(orderEntity, currentProductsSnapshot, existingOrderId)
                    Log.i("OrdersRedActivity", "Order save transaction successful for order ID: $savedOrderId")
                    Toast.makeText(this@OrdersRedActivity, "Заказ сохранен успешно", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Log.e("OrdersRedActivity", "Error during save order transaction", e)
                    Toast.makeText(this@OrdersRedActivity, "Ошибка сохранения заказа: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private data class AvailabilityCheckResult(val isAvailable: Boolean, val unavailableMessages: List<String>)

    private suspend fun checkProductAvailability(): AvailabilityCheckResult {
        var allProductsAvailable = true
        val unavailableProducts = mutableListOf<String>()
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
                    unavailableProducts.add("${item.product.name} (нужно ${item.quantityInOrder}, доступно $availableQuantity)")
                }
            }
        }
        return AvailabilityCheckResult(allProductsAvailable, unavailableProducts)
    }

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

}