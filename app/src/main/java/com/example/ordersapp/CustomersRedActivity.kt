package com.example.ordersapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ordersapp.databinding.ActivityCustomersRedBinding // Проверьте путь
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.CustomersDao
import com.example.ordersapp.db.CustomersEntity
import kotlinx.coroutines.launch
import java.lang.Exception

class CustomersRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomersRedBinding
    private lateinit var customersDao: CustomersDao
    private var currentCustomer: CustomersEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customersDao = AppDatabase.getDatabase(this).customersDao()

        val customerId = intent.getIntExtra("customer_id", -1)

        if (customerId != -1) {
            Log.d("CustomersRedActivity", "Editing customer with ID: $customerId")
            loadCustomerData(customerId)
        } else {
            Log.d("CustomersRedActivity", "Adding new customer.")
        }

        binding.btnSaveCustomer.setOnClickListener {
            saveCustomer()
        }
    }

    private fun loadCustomerData(customerId: Int) {
        lifecycleScope.launch {
            try {
                currentCustomer = customersDao.getCustomerById(customerId)
                currentCustomer?.let { customer ->
                    binding.edTextCustomerName.setText(customer.name)
                    binding.edTextCustomerAddress.setText(customer.address ?: "") // Загружаем адрес
                    binding.edTextCustomerPhone.setText(customer.phone)
                    binding.exTextCustomerEmail.setText(customer.contactPersonEmail)
                    Log.i("CustomersRedActivity", "Customer data loaded for ID: $customerId")
                } ?: run {
                    Log.w("CustomersRedActivity", "Customer with ID $customerId not found in DB.")
                    Toast.makeText(this@CustomersRedActivity, "Customer not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("CustomersRedActivity", "Error loading customer $customerId", e)
                Toast.makeText(this@CustomersRedActivity, "Error loading customer data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveCustomer() {
        val name = binding.edTextCustomerName.text.toString().trim()
        // Считываем адрес, если пустой - будет null
        val address = binding.edTextCustomerAddress.text.toString().trim().let { if (it.isEmpty()) null else it }
        val phone = binding.edTextCustomerPhone.text.toString().trim()
        val email = binding.exTextCustomerEmail.text.toString().trim()

        if (name.isEmpty()) {
            binding.edTextCustomerName.error = "Name cannot be empty"
            return
        }

        val customerToSave: CustomersEntity
        if (currentCustomer == null) {
            // --- ИСПРАВЛЕНО: Добавлен параметр address ---
            Log.d("CustomersRedActivity", "Creating new customer object.")
            customerToSave = CustomersEntity(
                idCustomer = 0,
                name = name,
                address = address, // Передаем адрес
                phone = phone,
                contactPersonEmail = email
            )
            // --------------------------------------------
        } else {
            // --- ИСПРАВЛЕНО: Добавлен параметр address в copy() ---
            Log.d("CustomersRedActivity", "Updating existing customer object with ID: ${currentCustomer!!.idCustomer}")
            customerToSave = currentCustomer!!.copy(
                name = name,
                address = address, // Передаем адрес
                phone = phone,
                contactPersonEmail = email
            )
            // ----------------------------------------------------
        }

        lifecycleScope.launch {
            try {
                if (currentCustomer == null) {
                    val newId = customersDao.insert(customerToSave)
                    Log.i("CustomersRedActivity", "New customer inserted with ID: $newId")
                    Toast.makeText(this@CustomersRedActivity, "Customer added", Toast.LENGTH_SHORT).show()
                } else {
                    val updatedRows = customersDao.update(customerToSave)
                    if (updatedRows > 0) {
                        Log.i("CustomersRedActivity", "Customer ${customerToSave.idCustomer} updated successfully.")
                        Toast.makeText(this@CustomersRedActivity, "Customer data updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w("CustomersRedActivity", "Customer ${customerToSave.idCustomer} not found for update or update failed.")
                        Toast.makeText(this@CustomersRedActivity, "Failed to update data", Toast.LENGTH_SHORT).show()
                    }
                }
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Log.e("CustomersRedActivity", "Error saving customer data", e)
                Toast.makeText(this@CustomersRedActivity, "Error saving data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}