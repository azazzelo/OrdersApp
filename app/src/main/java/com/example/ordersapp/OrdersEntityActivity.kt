package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.databinding.ActivityOrdersEntityBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.OrdersAdapter // Импортируем обновленный адаптер
import com.example.ordersapp.db.OrdersDao
import com.example.ordersapp.db.OrdersEntity
import com.example.ordersapp.db.OrdersProductsDao
// import com.example.ordersapp.db.CustomersDao // Больше не нужен здесь
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// import kotlinx.coroutines.Dispatchers // Не нужен для getCustomerName
// import kotlinx.coroutines.withContext // Не нужен для getCustomerName

class OrdersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersEntityBinding
    private lateinit var adapter: OrdersAdapter // Тип адаптера - наш обновленный OrdersAdapter
    private lateinit var ordersDao: OrdersDao
    private lateinit var ordersProductsDao: OrdersProductsDao
    // private lateinit var customersDao: CustomersDao // Убрали

    private var currentSearchQuery: String = ""
    private var currentSortBy: String = "dateRegistration"
    private var currentSortOrder: String = "DESC"
    private var searchJob: Job? = null

    private val orderActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadOrders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        ordersDao = db.ordersDao()
        ordersProductsDao = db.ordersProductsDao()
        // customersDao = db.customersDao() // Убрали инициализацию

        setupRecyclerView() // Вызываем ДО setupSearchAndSort
        setupSearchAndSort()

        binding.btnAddOrder.setOnClickListener {
            val intent = Intent(this, OrdersRedActivity::class.java)
            orderActivityResultLauncher.launch(intent)
        }

        loadOrders()
    }

    private fun setupRecyclerView() {
        // Создаем адаптер БЕЗ передачи getCustomerName
        adapter = OrdersAdapter(
            onItemClick = { order ->
                val intent = Intent(this, OrdersRedActivity::class.java).apply {
                    putExtra("order_id", order.idOrder)
                }
                orderActivityResultLauncher.launch(intent)
            },
            onItemDelete = { order ->
                deleteOrder(order)
            }
            // Лямбда getCustomerName удалена
        )
        binding.rcOrders.adapter = adapter
        binding.rcOrders.layoutManager = LinearLayoutManager(this)
        binding.rcOrders.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupSearchAndSort() {
        binding.searchViewOrders.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQueryChanged(query ?: "")
                binding.searchViewOrders.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    searchQueryChanged(newText ?: "")
                }
                return true
            }
        })

        binding.btnSortOrdersDesc.setOnClickListener {
            setSortOrder("dateRegistration", "DESC")
        }
        binding.btnSortOrdersAsc.setOnClickListener {
            setSortOrder("dateRegistration", "ASC")
        }
        updateSortButtonsState()
    }

    private fun searchQueryChanged(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery != currentSearchQuery) {
            currentSearchQuery = trimmedQuery
            Log.d("OrdersEntityActivity", "Search query changed: $currentSearchQuery")
            loadOrders()
        }
    }

    private fun setSortOrder(sortBy: String, sortOrder: String) {
        if (currentSortBy != sortBy || currentSortOrder != sortOrder) {
            currentSortBy = sortBy
            currentSortOrder = sortOrder
            Log.d("OrdersEntityActivity", "Sort order changed: By=$currentSortBy, Order=$currentSortOrder")
            updateSortButtonsState()
            loadOrders()
        }
    }

    private fun updateSortButtonsState() {
        val activeAlpha = 1.0f
        val inactiveAlpha = 0.5f
        if (currentSortBy == "dateRegistration") {
            binding.btnSortOrdersDesc.alpha = if (currentSortOrder == "DESC") activeAlpha else inactiveAlpha
            binding.btnSortOrdersAsc.alpha = if (currentSortOrder == "ASC") activeAlpha else inactiveAlpha
        } else {
            binding.btnSortOrdersDesc.alpha = inactiveAlpha
            binding.btnSortOrdersAsc.alpha = inactiveAlpha
        }
    }

    private fun loadOrders() {
        Log.d("OrdersEntityActivity", "Loading orders with query='$currentSearchQuery', sortBy=$currentSortBy, sortOrder=$currentSortOrder")
        lifecycleScope.launch {
            try {
                // Вызываем DAO без изменений
                val ordersList = ordersDao.getOrdersFilteredSorted(
                    searchQuery = currentSearchQuery,
                    startDate = null,
                    endDate = null,
                    sortBy = currentSortBy,
                    sortOrder = currentSortOrder
                )
                // Передаем список в адаптер
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
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete Order #${order.idOrder}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ordersProductsDao.deleteProductsForOrder(order.idOrder)
                        val deletedRows = ordersDao.deleteOrder(order)
                        if (deletedRows > 0) {
                            Toast.makeText(this@OrdersEntityActivity, "Order deleted", Toast.LENGTH_SHORT).show()
                            loadOrders() // Reload list
                        } else {
                            Toast.makeText(this@OrdersEntityActivity, "Failed to delete order", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersEntityActivity", "Error deleting order ${order.idOrder}", e)
                        Toast.makeText(this@OrdersEntityActivity, "Error deleting order", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}