package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.databinding.ActivityOrdersEntityBinding // Проверьте путь
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.OrdersAdapter
import com.example.ordersapp.db.OrdersDao
import com.example.ordersapp.db.OrdersEntity
import com.example.ordersapp.db.OrdersProductsDao // Нужен для удаления связей
import com.example.ordersapp.db.CustomersDao // Нужен для получения имени клиента
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class OrdersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersEntityBinding
    private lateinit var adapter: OrdersAdapter
    private lateinit var ordersDao: OrdersDao
    private lateinit var ordersProductsDao: OrdersProductsDao
    private lateinit var customersDao: CustomersDao // Добавили DAO клиентов

    // Регистратор для получения результата из OrdersRedActivity
    private val orderActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadOrders() // Перезагружаем список, если заказ был сохранен/изменен
            Log.d("OrdersEntityActivity", "Orders list reloaded after result OK.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем DAO
        val db = AppDatabase.getDatabase(this)
        ordersDao = db.ordersDao()
        ordersProductsDao = db.ordersProductsDao()
        customersDao = db.customersDao() // Инициализируем DAO клиентов

        setupRecyclerView()

        binding.btnAddOrder.setOnClickListener {
            // Запускаем OrdersRedActivity для добавления нового заказа
            val intent = Intent(this, OrdersRedActivity::class.java)
            orderActivityResultLauncher.launch(intent)
            Log.d("OrdersEntityActivity", "Starting OrdersRedActivity for add.")
        }

        loadOrders() // Первичная загрузка списка
    }

    private fun setupRecyclerView() {
        adapter = OrdersAdapter(
            onItemClick = { order ->
                // Клик на элемент - переход на редактирование
                Log.d("OrdersEntityActivity", "Clicked on order: ${order.idOrder}")
                val intent = Intent(this, OrdersRedActivity::class.java).apply {
                    putExtra("order_id", order.idOrder) // Передаем ID заказа
                }
                orderActivityResultLauncher.launch(intent)
            },
            onItemDelete = { order ->
                // Долгое нажатие - удаление
                Log.d("OrdersEntityActivity", "Long clicked on order: ${order.idOrder}")
                deleteOrder(order)
            },
            // Передаем лямбду для получения имени клиента
            getCustomerName = { customerId ->
                // Эта функция будет вызываться из адаптера асинхронно
                // Выполняем запрос к БД в фоновом потоке
                withContext(Dispatchers.IO) {
                    customersDao.getCustomerById(customerId)?.name
                }
            }
        )
        binding.rcOrders.adapter = adapter
        binding.rcOrders.layoutManager = LinearLayoutManager(this)
        // Добавляем разделитель
        binding.rcOrders.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            try {
                val ordersList = ordersDao.getAllOrders()
                adapter.submitList(ordersList)
                Log.d("OrdersEntityActivity", "Orders loaded: ${ordersList.size} items.")
            } catch (e: Exception) {
                Log.e("OrdersEntityActivity", "Error loading orders", e)
                Toast.makeText(this@OrdersEntityActivity, "Ошибка загрузки заказов", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteOrder(order: OrdersEntity) {
        AlertDialog.Builder(this)
            .setTitle("Удаление заказа")
            .setMessage("Вы уверены, что хотите удалить заказ №${order.idOrder}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Сначала удаляем связанные записи в Orders-Products
                        ordersProductsDao.deleteProductsForOrder(order.idOrder)
                        Log.d("OrdersEntityActivity", "Deleted related products for order ${order.idOrder}")

                        // Затем удаляем сам заказ
                        val deletedRows = ordersDao.deleteOrder(order)

                        if (deletedRows > 0) {
                            Log.i("OrdersEntityActivity", "Order ${order.idOrder} deleted successfully.")
                            Toast.makeText(this@OrdersEntityActivity, "Заказ удален", Toast.LENGTH_SHORT).show()
                            loadOrders() // Перезагружаем список
                        } else {
                            Log.w("OrdersEntityActivity", "Order ${order.idOrder} not found for deletion or delete failed.")
                            Toast.makeText(this@OrdersEntityActivity, "Не удалось удалить заказ", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersEntityActivity", "Error deleting order ${order.idOrder}", e)
                        Toast.makeText(this@OrdersEntityActivity, "Ошибка удаления заказа", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}