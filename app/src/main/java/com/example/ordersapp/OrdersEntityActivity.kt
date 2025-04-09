package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // Импорт SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.databinding.ActivityOrdersEntityBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.OrdersAdapter
import com.example.ordersapp.db.OrdersDao
import com.example.ordersapp.db.OrdersEntity
import com.example.ordersapp.db.OrdersProductsDao
import com.example.ordersapp.db.CustomersDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job // Импорт Job для отмены поиска
import kotlinx.coroutines.delay // Импорт delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersEntityBinding
    private lateinit var adapter: OrdersAdapter
    private lateinit var ordersDao: OrdersDao
    private lateinit var ordersProductsDao: OrdersProductsDao
    private lateinit var customersDao: CustomersDao

    // --- Параметры для запроса ---
    private var currentSearchQuery: String = ""
    private var currentSortBy: String = "dateRegistration" // По умолчанию - дата регистрации
    private var currentSortOrder: String = "DESC" // По умолчанию - новые сверху
    private var searchJob: Job? = null // Для отложенного поиска

    private val orderActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadOrders() // Перезагружаем с текущими параметрами
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        ordersDao = db.ordersDao()
        ordersProductsDao = db.ordersProductsDao()
        customersDao = db.customersDao()

        setupRecyclerView()
        setupSearchAndSort() // Настраиваем поиск и сортировку

        binding.btnAddOrder.setOnClickListener {
            val intent = Intent(this, OrdersRedActivity::class.java)
            orderActivityResultLauncher.launch(intent)
        }

        loadOrders() // Первичная загрузка
    }

    private fun setupRecyclerView() {
        adapter = OrdersAdapter(
            onItemClick = { order ->
                val intent = Intent(this, OrdersRedActivity::class.java).apply {
                    putExtra("order_id", order.idOrder)
                }
                orderActivityResultLauncher.launch(intent)
            },
            onItemDelete = { order ->
                deleteOrder(order)
            },
            getCustomerName = { customerId ->
                withContext(Dispatchers.IO) {
                    customersDao.getCustomerById(customerId)?.name
                }
            }
        )
        binding.rcOrders.adapter = adapter
        binding.rcOrders.layoutManager = LinearLayoutManager(this)
        binding.rcOrders.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    // Настройка SearchView и кнопок сортировки
    private fun setupSearchAndSort() {
        // Поиск
        binding.searchViewOrders.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Обычно не используется при поиске "на лету"
                searchQueryChanged(query ?: "")
                binding.searchViewOrders.clearFocus() // Скрываем клавиатуру
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Используем debounce для отложенного поиска
                searchJob?.cancel() // Отменяем предыдущий запланированный поиск
                searchJob = lifecycleScope.launch {
                    delay(300) // Ждем 300 мс после последнего ввода
                    searchQueryChanged(newText ?: "")
                }
                return true
            }
        })

        // Кнопки сортировки
        binding.btnSortOrdersDesc.setOnClickListener {
            setSortOrder("dateRegistration", "DESC")
        }
        binding.btnSortOrdersAsc.setOnClickListener {
            setSortOrder("dateRegistration", "ASC")
        }
        // Обновляем вид кнопок сортировки при старте
        updateSortButtonsState()
    }

    // Вызывается при изменении текста поиска
    private fun searchQueryChanged(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery != currentSearchQuery) {
            currentSearchQuery = trimmedQuery
            Log.d("OrdersEntityActivity", "Search query changed: $currentSearchQuery")
            loadOrders() // Перезагружаем список с новым поиском
        }
    }

    // Вызывается при нажатии кнопок сортировки
    private fun setSortOrder(sortBy: String, sortOrder: String) {
        if (currentSortBy != sortBy || currentSortOrder != sortOrder) {
            currentSortBy = sortBy
            currentSortOrder = sortOrder
            Log.d("OrdersEntityActivity", "Sort order changed: By=$currentSortBy, Order=$currentSortOrder")
            updateSortButtonsState() // Обновляем вид кнопок
            loadOrders() // Перезагружаем список с новой сортировкой
        }
    }

    // Визуально показывает активную сортировку (например, меняя цвет)
    private fun updateSortButtonsState() {
        // Простой пример: делаем активную кнопку чуть ярче (или меняем tint)
        val activeAlpha = 1.0f
        val inactiveAlpha = 0.5f

        if (currentSortBy == "dateRegistration") {
            binding.btnSortOrdersDesc.alpha = if (currentSortOrder == "DESC") activeAlpha else inactiveAlpha
            binding.btnSortOrdersAsc.alpha = if (currentSortOrder == "ASC") activeAlpha else inactiveAlpha
        } else {
            // Если сортировка по другому полю, обе кнопки неактивны
            binding.btnSortOrdersDesc.alpha = inactiveAlpha
            binding.btnSortOrdersAsc.alpha = inactiveAlpha
        }
        // Можно добавить другие кнопки и логику для сортировки по dateDelivery, finallyCost
    }


    // Метод загрузки теперь использует параметры
    private fun loadOrders() {
        Log.d("OrdersEntityActivity", "Loading orders with query='$currentSearchQuery', sortBy=$currentSortBy, sortOrder=$currentSortOrder")
        lifecycleScope.launch {
            try {
                val ordersList = ordersDao.getOrdersFilteredSorted(
                    searchQuery = currentSearchQuery,
                    startDate = null, // Фильтр по дате не реализован в UI
                    endDate = null,   // Фильтр по дате не реализован в UI
                    sortBy = currentSortBy,
                    sortOrder = currentSortOrder
                )
                adapter.submitList(ordersList)
                Log.d("OrdersEntityActivity", "Orders loaded: ${ordersList.size} items.")
            } catch (e: Exception) {
                Log.e("OrdersEntityActivity", "Error loading orders", e)
                Toast.makeText(this@OrdersEntityActivity, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteOrder(order: OrdersEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Order") // English
            .setMessage("Are you sure you want to delete Order #${order.idOrder}?") // English
            .setPositiveButton("Delete") { _, _ -> // English
                lifecycleScope.launch {
                    try {
                        ordersProductsDao.deleteProductsForOrder(order.idOrder)
                        val deletedRows = ordersDao.deleteOrder(order)
                        if (deletedRows > 0) {
                            Toast.makeText(this@OrdersEntityActivity, "Order deleted", Toast.LENGTH_SHORT).show() // English
                            loadOrders() // Reload list
                        } else {
                            Toast.makeText(this@OrdersEntityActivity, "Failed to delete order", Toast.LENGTH_SHORT).show() // English
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersEntityActivity", "Error deleting order ${order.idOrder}", e)
                        Toast.makeText(this@OrdersEntityActivity, "Error deleting order", Toast.LENGTH_LONG).show() // English
                    }
                }
            }
            .setNegativeButton("Cancel", null) // English
            .show()
    }
}