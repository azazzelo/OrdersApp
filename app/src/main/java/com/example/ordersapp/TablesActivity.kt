package com.example.ordersapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.ordersapp.databinding.ActivityTablesBinding

class TablesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTablesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTablesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val roleId = sharedPrefs.getInt("roleId", -1)

        // Настройка видимости кнопок по роли
        when (roleId) {
            1 -> setupDirectorUI()   // Директор
            2 -> setupDispatcherUI() // Диспетчер
            3 -> setupAccountantUI() // Бухгалтер
            else -> finish()         // Неизвестная роль
        }

        // Обработчики кнопок навигации
        setupNavigationButtons()

        // Кнопка "Назад" - возврат на авторизацию
        binding.btnTablesBack.setOnClickListener {
            navigateToAuth()
        }
    }

    private fun setupDirectorUI() {
        // Все кнопки видны для директора
        binding.btnTableRoles.visibility = View.VISIBLE
        binding.btnTableUsers.visibility = View.VISIBLE
        binding.btnTableCustomers.visibility = View.VISIBLE
        binding.btnTableOrders.visibility = View.VISIBLE
        binding.btnTableOrdersProducts.visibility = View.VISIBLE
        binding.btnTableProducts.visibility = View.VISIBLE
    }

    private fun setupDispatcherUI() {
        // Доступ только к товарам, заказам и заказчикам
        binding.btnTableRoles.visibility = View.GONE
        binding.btnTableUsers.visibility = View.GONE
        binding.btnTableOrdersProducts.visibility = View.GONE

        binding.btnTableCustomers.visibility = View.VISIBLE
        binding.btnTableOrders.visibility = View.VISIBLE
        binding.btnTableProducts.visibility = View.VISIBLE
    }

    private fun setupAccountantUI() {
        // Доступ только к товарам и заказам
        binding.btnTableRoles.visibility = View.GONE
        binding.btnTableUsers.visibility = View.GONE
        binding.btnTableCustomers.visibility = View.GONE
        binding.btnTableOrdersProducts.visibility = View.GONE

        binding.btnTableOrders.visibility = View.VISIBLE
        binding.btnTableProducts.visibility = View.VISIBLE
    }

    private fun setupNavigationButtons() {
        binding.btnTableRoles.setOnClickListener {
            startActivity(Intent(this, RolesEntityActivity::class.java))
        }
        binding.btnTableUsers.setOnClickListener {
            startActivity(Intent(this, UsersEntityActivity::class.java))
        }
        binding.btnTableCustomers.setOnClickListener {
            startActivity(Intent(this, CustomersEntityActivity::class.java))
        }
        binding.btnTableOrders.setOnClickListener {
            startActivity(Intent(this, OrdersEntityActivity::class.java))
        }
        binding.btnTableOrdersProducts.setOnClickListener {
            startActivity(Intent(this, AddProductInOrderActivity::class.java))
        }
        binding.btnTableProducts.setOnClickListener {
            startActivity(Intent(this, ProductsEntityActivity::class.java))
        }
    }

    private fun navigateToAuth() {
        // Очищаем данные авторизации
        getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply()

        // Возвращаемся на MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

}