package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.ordersapp.adapters.AvailableProductAdapter // Проверьте путь
import com.example.ordersapp.databinding.ActivityAddProductInOrderBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.ProductsDao
import com.example.ordersapp.db.ProductsEntity
import kotlinx.coroutines.launch

class AddProductInOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductInOrderBinding
    private lateinit var productsDao: ProductsDao
    private lateinit var adapter: AvailableProductAdapter
    private var existingProductIds = intArrayOf() // ID товаров, уже добавленных в заказ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductInOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productsDao = AppDatabase.getDatabase(this).productsDao()
        existingProductIds = intent.getIntArrayExtra("existing_product_ids") ?: intArrayOf()

        setupRecyclerView()
        loadAvailableProducts()
    }

    private fun setupRecyclerView() {
        adapter = AvailableProductAdapter { product ->
            // Клик на товар - показываем диалог ввода количества
            showQuantityDialog(product)
        }
        binding.rcAvailableProducts.adapter = adapter
        binding.rcAvailableProducts.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadAvailableProducts() {
        binding.progressBarAddProduct.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val allAvailable = productsDao.getAvailableProducts()
                // Фильтруем, убирая те, что уже есть в заказе
                val filteredList = allAvailable.filterNot { existingProductIds.contains(it.idProduct) }
                adapter.submitList(filteredList)
                binding.progressBarAddProduct.visibility = View.GONE
                if(filteredList.isEmpty()) {
                    binding.tvAddProductTitle.text = "Нет доступных товаров для добавления"
                }
                Log.d("AddProductActivity", "Loaded ${filteredList.size} available products.")
            } catch (e: Exception) {
                binding.progressBarAddProduct.visibility = View.GONE
                Log.e("AddProductActivity", "Error loading available products", e)
                Toast.makeText(this@AddProductInOrderActivity, "Ошибка загрузки товаров", Toast.LENGTH_SHORT).show()
                // Можно закрыть активность, если список не загрузился
                finish()
            }
        }
    }

    private fun showQuantityDialog(product: ProductsEntity) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Макс: ${product.quantity}" // Подсказка с макс. количеством
        }

        AlertDialog.Builder(this)
            .setTitle("Количество для '${product.name}'")
            .setMessage("Введите количество (доступно: ${product.quantity}):")
            .setView(editText)
            .setPositiveButton("Добавить") { dialog, _ ->
                val quantityString = editText.text.toString()
                val quantity = quantityString.toIntOrNull()

                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, "Введите корректное количество (> 0)", Toast.LENGTH_SHORT).show()
                    // Не закрываем диалог, или вызываем showQuantityDialog снова
                } else if (quantity > product.quantity) {
                    Toast.makeText(this, "Нельзя добавить больше, чем есть на складе (${product.quantity})", Toast.LENGTH_SHORT).show()
                    // Не закрываем диалог
                } else {
                    // Количество корректно - возвращаем результат
                    Log.d("AddProductActivity", "Quantity valid: $quantity for product ID: ${product.idProduct}")
                    val resultIntent = Intent().apply {
                        putExtra("selected_product_id", product.idProduct)
                        putExtra("selected_quantity", quantity)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish() // Закрываем активность выбора товара
                }
                // dialog.dismiss() // Можно убрать, чтобы диалог не закрывался при ошибке
            }
            .setNegativeButton("Отмена", null) // Просто закрывает диалог
            .show()
    }
}