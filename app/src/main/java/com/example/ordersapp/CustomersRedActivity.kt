package com.example.ordersapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ordersapp.databinding.ActivityCustomersRedBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.CustomersDao
import com.example.ordersapp.db.CustomersEntity
import kotlinx.coroutines.launch
import java.lang.Exception

class CustomersRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomersRedBinding
    private lateinit var customersDao: CustomersDao
    private var currentCustomer: CustomersEntity? = null // Хранит данные редактируемого заказчика

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customersDao = AppDatabase.getDatabase(this).customersDao()

        // Получаем ID заказчика из Intent
        val customerId = intent.getIntExtra("customer_id", -1)

        if (customerId != -1) {
            // Если ID есть, загружаем данные для редактирования
            Log.d("CustomersRedActivity", "Editing customer with ID: $customerId")
            loadCustomerData(customerId)
        } else {
            Log.d("CustomersRedActivity", "Adding new customer.")
            // Иначе это добавление нового заказчика
            // Поля остаются пустыми
        }

        // Устанавливаем слушатель на кнопку сохранения
        binding.btnSaveCustomer.setOnClickListener {
            saveCustomer()
        }
    }

    private fun loadCustomerData(customerId: Int) {
        lifecycleScope.launch {
            try {
                currentCustomer = customersDao.getCustomerById(customerId)
                currentCustomer?.let { customer ->
                    // Заполняем поля данными
                    binding.edTextCustomerName.setText(customer.name)
                    binding.edTextCustomerPhone.setText(customer.phone)
                    binding.exTextCustomerEmail.setText(customer.contactPersonEmail)
                    Log.i("CustomersRedActivity", "Customer data loaded for ID: $customerId")
                } ?: run {
                    Log.w("CustomersRedActivity", "Customer with ID $customerId not found in DB.")
                    Toast.makeText(this@CustomersRedActivity, "Заказчик не найден", Toast.LENGTH_SHORT).show()
                    finish() // Закрываем, если не нашли
                }
            } catch (e: Exception) {
                Log.e("CustomersRedActivity", "Error loading customer $customerId", e)
                Toast.makeText(this@CustomersRedActivity, "Ошибка загрузки данных заказчика", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveCustomer() {
        // Считываем данные из полей
        val name = binding.edTextCustomerName.text.toString().trim()
        val phone = binding.edTextCustomerPhone.text.toString().trim()
        val email = binding.exTextCustomerEmail.text.toString().trim()

        // --- Валидация ---
        if (name.isEmpty()) {
            binding.edTextCustomerName.error = "Имя не может быть пустым"
            return // Прерываем сохранение
        }
        // Можно добавить другие валидации (например, формат email, телефона)
        // if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.isNotEmpty()) {
        //    binding.exTextCustomerEmail.error = "Некорректный формат email"
        //    return
        // }

        // Создаем или обновляем объект CustomersEntity
        val customerToSave: CustomersEntity
        if (currentCustomer == null) {
            // Создание нового
            Log.d("CustomersRedActivity", "Creating new customer object.")
            customerToSave = CustomersEntity(
                idCustomer = 0, // ID будет сгенерирован базой данных
                name = name,
                phone = phone,
                contactPersonEmail = email
            )
        } else {
            // Обновление существующего
            Log.d("CustomersRedActivity", "Updating existing customer object with ID: ${currentCustomer!!.idCustomer}")
            customerToSave = currentCustomer!!.copy(
                name = name,
                phone = phone,
                contactPersonEmail = email
            )
        }

        // Сохраняем в базе данных в корутине
        lifecycleScope.launch {
            try {
                if (currentCustomer == null) {
                    // Вставляем нового
                    val newId = customersDao.insert(customerToSave)
                    Log.i("CustomersRedActivity", "New customer inserted with ID: $newId")
                    Toast.makeText(this@CustomersRedActivity, "Заказчик добавлен", Toast.LENGTH_SHORT).show()
                } else {
                    // Обновляем существующего
                    val updatedRows = customersDao.update(customerToSave)
                    if (updatedRows > 0) {
                        Log.i("CustomersRedActivity", "Customer ${customerToSave.idCustomer} updated successfully.")
                        Toast.makeText(this@CustomersRedActivity, "Данные обновлены", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w("CustomersRedActivity", "Customer ${customerToSave.idCustomer} not found for update or update failed.")
                        Toast.makeText(this@CustomersRedActivity, "Не удалось обновить данные", Toast.LENGTH_SHORT).show()
                    }
                }
                // Устанавливаем результат OK, чтобы CustomersEntityActivity обновил список
                setResult(Activity.RESULT_OK)
                finish() // Закрываем активность после сохранения
            } catch (e: Exception) {
                Log.e("CustomersRedActivity", "Error saving customer data", e)
                Toast.makeText(this@CustomersRedActivity, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show()
            }
        }
    }
}