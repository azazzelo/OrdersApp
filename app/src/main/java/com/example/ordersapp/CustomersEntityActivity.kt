package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // Для нового способа получения результата
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.databinding.ActivityCustomersEntityBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.CustomersAdapter
import com.example.ordersapp.db.CustomersDao
import com.example.ordersapp.db.CustomersEntity
import kotlinx.coroutines.launch
import java.lang.Exception

class CustomersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomersEntityBinding
    private lateinit var adapter: CustomersAdapter
    private lateinit var customersDao: CustomersDao

    // Новый способ получения результата из CustomersRedActivity
    private val customerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Если данные были успешно сохранены (добавлены/обновлены),
            // перезагружаем список
            loadCustomers()
            Log.d("CustomersEntityActivity", "Customer list reloaded after result OK.")
        } else {
            Log.d("CustomersEntityActivity", "Result code from CustomersRedActivity: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customersDao = AppDatabase.getDatabase(this).customersDao()
        setupRecyclerView()

        binding.btnAddCustomer.setOnClickListener {
            // Запускаем CustomersRedActivity для добавления нового заказчика
            val intent = Intent(this, CustomersRedActivity::class.java)
            customerActivityResultLauncher.launch(intent)
            Log.d("CustomersEntityActivity", "Starting CustomersRedActivity for add.")
        }

        loadCustomers() // Первичная загрузка списка
    }

    // Можно убрать onResume, так как используем ActivityResultLauncher
    // override fun onResume() {
    //    super.onResume()
    //    loadCustomers()
    // }

    private fun setupRecyclerView() {
        adapter = CustomersAdapter(
            onItemClick = { customer ->
                // Обработка клика на элемент - переход на редактирование
                Log.d("CustomersEntityActivity", "Clicked on customer: ${customer.idCustomer}")
                val intent = Intent(this, CustomersRedActivity::class.java).apply {
                    putExtra("customer_id", customer.idCustomer) // Передаем ID заказчика
                }
                customerActivityResultLauncher.launch(intent)
            },
            onItemDelete = { customer ->
                // Обработка долгого нажатия - удаление
                Log.d("CustomersEntityActivity", "Long clicked on customer: ${customer.idCustomer}")
                deleteCustomer(customer)
            }
        )
        binding.rcCustomers.adapter = adapter
        binding.rcCustomers.layoutManager = LinearLayoutManager(this)
        // Добавляем разделитель между элементами
        binding.rcCustomers.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadCustomers() {
        lifecycleScope.launch {
            try {
                val customersList = customersDao.getAllCustomers()
                adapter.submitList(customersList)
                Log.d("CustomersEntityActivity", "Customers loaded: ${customersList.size} items.")
            } catch (e: Exception) {
                Log.e("CustomersEntityActivity", "Error loading customers", e)
                Toast.makeText(this@CustomersEntityActivity, "Ошибка загрузки заказчиков", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCustomer(customer: CustomersEntity) {
        AlertDialog.Builder(this)
            .setTitle("Удаление заказчика")
            .setMessage("Вы уверены, что хотите удалить заказчика '${customer.name}'?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val deletedRows = customersDao.delete(customer)
                        if (deletedRows > 0) {
                            Log.i("CustomersEntityActivity", "Customer ${customer.idCustomer} deleted successfully.")
                            Toast.makeText(this@CustomersEntityActivity, "Заказчик удален", Toast.LENGTH_SHORT).show()
                            loadCustomers() // Перезагружаем список после удаления
                        } else {
                            Log.w("CustomersEntityActivity", "Customer ${customer.idCustomer} not found for deletion or delete failed.")
                            Toast.makeText(this@CustomersEntityActivity, "Не удалось удалить заказчика", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // Обработка ошибок, например, если есть связанные заказы (FOREIGN KEY constraint)
                        Log.e("CustomersEntityActivity", "Error deleting customer ${customer.idCustomer}", e)
                        Toast.makeText(this@CustomersEntityActivity, "Ошибка удаления: возможно, есть связанные заказы", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}