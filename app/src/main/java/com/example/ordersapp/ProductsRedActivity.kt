package com.example.ordersapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide // Импортируем Glide
import com.example.ordersapp.databinding.ActivityProductsRedBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.ProductsDao
import com.example.ordersapp.db.ProductsEntity
import kotlinx.coroutines.launch

class ProductsRedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductsRedBinding
    private lateinit var productsDao: ProductsDao
    private var currentProduct: ProductsEntity? = null
    private var selectedImageUri: Uri? = null // Этот URI будет использоваться для СОХРАНЕНИЯ

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            Log.d("ProductsRedActivity", "Image selected: $it")
            // Пытаемся получить постоянные права (может не сработать для всех URI)
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
                Log.i("ProductsRedActivity", "Persistable URI permission grant successful for: $it")
                selectedImageUri = it // Сохраняем URI для последующего сохранения в БД

                // Используем Glide для отображения выбранного изображения
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.imageView)
                binding.imageView.visibility = View.VISIBLE

            } catch (e: SecurityException) {
                Log.e("ProductsRedActivity", "Failed to take persistable URI permission for: $it", e)
                // Если не удалось получить права, URI может быть временным.
                // Отобразить все равно можно, но сохранить надежно не получится.
                // Лучше копировать файл в таком случае, но пока просто покажем Toast.
                selectedImageUri = it // Сохраняем URI, но он может быть временным
                Glide.with(this).load(it).into(binding.imageView)
                binding.imageView.visibility = View.VISIBLE
                Toast.makeText(this, "Внимание: Доступ к фото может быть утерян", Toast.LENGTH_LONG).show()
            } catch (e: Exception){
                Log.e("ProductsRedActivity", "Error processing selected image URI: $it", e)
                Toast.makeText(this, "Не удалось обработать выбранное фото", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.d("ProductsRedActivity", "Image selection cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsRedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productsDao = AppDatabase.getDatabase(this).productsDao()

        binding.floatingActionButton2.setOnClickListener {
            pickImage.launch("image/*")
        }

        val productId = intent.getIntExtra("product_id", -1)
        if (productId != -1) {
            loadProductData(productId)
        } else {
            binding.imageView.visibility = View.GONE
        }

        binding.floatingActionButton.setOnClickListener {
            saveProduct()
        }
    }

    private fun loadProductData(productId: Int) {
        lifecycleScope.launch {
            Log.d("ProductsRedActivity", "Loading product with ID: $productId")
            try {
                currentProduct = productsDao.getProductById(productId)
                currentProduct?.let { product ->
                    binding.edTextProductName.setText(product.name)
                    binding.edTextProductCost.setText(product.cost.toString())
                    binding.edTextProductQuantity.setText(product.quantity.toString())

                    product.photoUri?.let { uriString ->
                        Log.d("ProductsRedActivity", "Product has photoUri: $uriString")
                        try {
                            // Не сохраняем этот URI в selectedImageUri, он только для отображения
                            val displayUri = Uri.parse(uriString)
                            selectedImageUri = displayUri // Сохраняем URI для возможности перезаписи если фото не менялось

                            Glide.with(this@ProductsRedActivity)
                                .load(displayUri)
                                .placeholder(R.drawable.ic_launcher_background) // Замените на свои placeholder'ы
                                .error(R.drawable.ic_launcher_foreground) // Замените на свои error drawable
                                .into(binding.imageView)
                            binding.imageView.visibility = View.VISIBLE
                            Log.i("ProductsRedActivity", "Successfully loaded image with Glide: $uriString")

                        } catch (e: Exception) {
                            Log.e("ProductsRedActivity", "Error parsing or loading image URI with Glide: $uriString", e)
                            binding.imageView.visibility = View.GONE
                            // Не показываем Toast здесь, так как Glide сам покажет error drawable
                            // Toast.makeText(this@ProductsRedActivity, "Ошибка загрузки сохраненного изображения", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.d("ProductsRedActivity", "Product has no photoUri")
                        binding.imageView.visibility = View.GONE
                    }
                } ?: run {
                    Log.w("ProductsRedActivity", "Product with ID $productId not found")
                    Toast.makeText(this@ProductsRedActivity, "Товар не найден", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("ProductsRedActivity", "Error loading product $productId from DB", e)
                Toast.makeText(this@ProductsRedActivity, "Ошибка загрузки товара из БД", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun saveProduct() {
        val name = binding.edTextProductName.text.toString().trim()
        val cost = binding.edTextProductCost.text.toString().toDoubleOrNull() ?: 0.0
        val quantity = binding.edTextProductQuantity.text.toString().toIntOrNull() ?: 0

        if (name.isEmpty()) {
            binding.edTextProductName.error = "Введите название"
            return
        }

        // Используем selectedImageUri, который был установлен либо при выборе нового фото,
        // либо при загрузке существующих данных (если фото было)
        val photoUriString = selectedImageUri?.toString()
        Log.d("ProductsRedActivity", "Saving product with photoUri: $photoUriString")


        val productToSave = currentProduct?.copy(
            name = name,
            cost = cost,
            quantity = quantity,
            photoUri = photoUriString // Сохраняем URI как строку
        ) ?: ProductsEntity(
            idProduct = 0,
            name = name,
            cost = cost,
            quantity = quantity,
            photoUri = photoUriString
        )

        lifecycleScope.launch {
            try {
                if (currentProduct == null) {
                    productsDao.insert(productToSave)
                    Log.i("ProductsRedActivity", "Product inserted successfully.")
                    Toast.makeText(this@ProductsRedActivity, "Товар добавлен", Toast.LENGTH_SHORT).show()
                } else {
                    productsDao.update(productToSave)
                    Log.i("ProductsRedActivity", "Product updated successfully.")
                    Toast.makeText(this@ProductsRedActivity, "Товар обновлен", Toast.LENGTH_SHORT).show()
                }
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Log.e("ProductsRedActivity", "Error saving product to DB", e)
                Toast.makeText(this@ProductsRedActivity, "Ошибка сохранения товара в БД", Toast.LENGTH_SHORT).show()
            }
        }
    }
}