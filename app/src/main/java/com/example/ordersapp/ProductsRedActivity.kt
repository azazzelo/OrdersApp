package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.ordersapp.databinding.ActivityProductsRedBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.ProductsDao
import com.example.ordersapp.db.ProductsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File // Импорт для File






class ProductsRedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductsRedBinding
    private lateinit var productsDao: ProductsDao
    private var currentProduct: ProductsEntity? = null
    // URI, который будет СОХРАНЕН в БД (указывает на файл во внутреннем хранилище)
    private var internalImageUri: Uri? = null
    // URI, выбранный пользователем (временный, для отображения и копирования)
    private var selectedContentUri: Uri? = null


    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { contentUri ->
            Log.d("ProductsRedActivity", "Image selected: $contentUri")
            selectedContentUri = contentUri // Сохраняем выбранный URI

            // Отображаем выбранное изображение немедленно
            Glide.with(this)
                .load(contentUri)
                .placeholder(R.drawable.ic_launcher_background) // Замените
                .error(R.drawable.ic_launcher_foreground)     // Замените
                .into(binding.imageView)
            binding.imageView.visibility = View.VISIBLE

            // Запускаем копирование в фоновом потоке
            // Результат копирования (internalImageUri) будет использован при СОХРАНЕНИИ товара
            copySelectedImageToInternalStorage(contentUri)

        } ?: run {
            Log.d("ProductsRedActivity", "Image selection cancelled")
        }
    }

    // Новая функция для копирования в фоне
    private fun copySelectedImageToInternalStorage(contentUri: Uri) {
        lifecycleScope.launch {
            // Показываем прогресс (опционально)
            // binding.imageCopyProgressBar.visibility = View.VISIBLE
            Log.d("ProductsRedActivity", "Starting copy process for $contentUri")
            val copiedUri = withContext(Dispatchers.IO) { // Выполняем копирование в IO потоке
                // Пытаемся получить постоянные права на всякий случай,
                // хотя для копирования они могут и не понадобиться, если временный доступ еще есть
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(contentUri, takeFlags)
                    Log.i("ProductsRedActivity", "Persistable read permission possibly granted for $contentUri")
                } catch (e: SecurityException) {
                    Log.w("ProductsRedActivity", "Failed to take persistable permission for $contentUri (SecurityException), proceeding with copy attempt.")
                } catch (e: Exception) {
                    Log.e("ProductsRedActivity", "Error trying to take persistable permission for $contentUri", e)
                }

                // Вызываем нашу функцию копирования
                copyUriToInternalStorage(this@ProductsRedActivity, contentUri)
            }
            // Скрываем прогресс
            // binding.imageCopyProgressBar.visibility = View.GONE

            if (copiedUri != null) {
                // Успешно скопировано! Сохраняем URI на ВНУТРЕННИЙ файл.
                // Удаляем старый файл, если он был и мы сейчас обновляем продукт
                cleanupOldInternalImage(internalImageUri?.toString()) // Удаляем предыдущий internal Uri, если был
                internalImageUri = copiedUri // Сохраняем новый internal Uri
                Log.i("ProductsRedActivity", "Image copied successfully to internal URI: $internalImageUri")
            } else {
                // Ошибка копирования
                internalImageUri = null // Сбрасываем URI
                selectedContentUri = null // Сбрасываем и выбранный, раз не скопировали
                Toast.makeText(this@ProductsRedActivity, "Не удалось сохранить выбранное фото", Toast.LENGTH_LONG).show()
                // Можно скрыть imageView или показать заглушку, если копирование не удалось
                binding.imageView.visibility = View.GONE
                Log.e("ProductsRedActivity", "Failed to copy image to internal storage.")
            }
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
                        Log.d("ProductsRedActivity", "Product has photoUri (internal): $uriString")
                        try {
                            // Этот URI указывает на файл во ВНУТРЕННЕМ хранилище
                            val displayUri = Uri.parse(uriString)
                            // Проверяем существует ли файл перед загрузкой
                            val fileExists = withContext(Dispatchers.IO) {
                                try { File(displayUri.path!!).exists() } catch (e: Exception) { false }
                            }

                            if(fileExists){
                                internalImageUri = displayUri // Сохраняем текущий внутренний URI
                                Glide.with(this@ProductsRedActivity)
                                    .load(displayUri) // Glide умеет работать с file:// URI
                                    .placeholder(R.drawable.ic_launcher_background) // Замените
                                    .error(R.drawable.ic_launcher_foreground)     // Замените
                                    .into(binding.imageView)
                                binding.imageView.visibility = View.VISIBLE
                                Log.i("ProductsRedActivity", "Successfully loaded internal image with Glide: $uriString")
                            } else {
                                Log.w("ProductsRedActivity", "Internal image file not found: $uriString. Clearing image.")
                                binding.imageView.visibility = View.GONE
                                internalImageUri = null // Сбрасываем, т.к. файл не найден
                                // Опционально: можно удалить битую ссылку из БД
                                // lifecycleScope.launch { productsDao.update(product.copy(photoUri = null)) }
                            }

                        } catch (e: Exception) {
                            Log.e("ProductsRedActivity", "Error parsing or loading internal image URI: $uriString", e)
                            binding.imageView.visibility = View.GONE
                            internalImageUri = null // Сбрасываем, если URI битый
                        }
                    } ?: run {
                        Log.d("ProductsRedActivity", "Product has no photoUri")
                        binding.imageView.visibility = View.GONE
                        internalImageUri = null // У продукта нет фото
                    }
                } ?: run {
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

        // ВАЖНО: Сохраняем URI на ВНУТРЕННИЙ файл (internalImageUri)
        val photoUriStringToSave = internalImageUri?.toString()
        Log.d("ProductsRedActivity", "Saving product. Current internal photoUri: $photoUriStringToSave")

        // --- Логика очистки старого файла ---
        val oldUriStringFromDb = currentProduct?.photoUri // URI, который был в БД до редактирования
        // Удаляем старый файл ТОЛЬКО если:
        // 1. Это редактирование (currentProduct != null)
        // 2. Старый URI существовал (oldUriStringFromDb != null)
        // 3. Новый URI либо null, либо отличается от старого
        if (currentProduct != null && oldUriStringFromDb != null && oldUriStringFromDb != photoUriStringToSave) {
            Log.d("ProductsRedActivity", "Need to cleanup old image: $oldUriStringFromDb")
            cleanupOldInternalImage(oldUriStringFromDb)
        } else {
            Log.d("ProductsRedActivity", "No need to cleanup old image. Old: $oldUriStringFromDb, New: $photoUriStringToSave")
        }


        val productToSave = currentProduct?.copy(
            name = name,
            cost = cost,
            quantity = quantity,
            photoUri = photoUriStringToSave // Сохраняем URI внутреннего файла
        ) ?: ProductsEntity(
            idProduct = 0,
            name = name,
            cost = cost,
            quantity = quantity,
            photoUri = photoUriStringToSave
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
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Log.e("ProductsRedActivity", "Error saving product to DB", e)
                Toast.makeText(this@ProductsRedActivity, "Ошибка сохранения товара в БД", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Функция для удаления файла изображения из внутреннего хранилища
    private fun cleanupOldInternalImage(oldUriString: String?) {
        if (oldUriString == null) return
        lifecycleScope.launch(Dispatchers.IO) { // Удаление файла - IO операция
            try {
                val oldUri = Uri.parse(oldUriString)
                // Убеждаемся, что это file URI из нашего хранилища
                if (oldUri.scheme == "file" && oldUri.path?.startsWith(filesDir.absolutePath) == true) {
                    val oldFile = File(oldUri.path!!)
                    if (oldFile.exists()) {
                        if (oldFile.delete()) {
                            Log.i("ProductsRedActivity", "Old image file deleted: ${oldFile.absolutePath}")
                        } else {
                            Log.w("ProductsRedActivity", "Failed to delete old image file: ${oldFile.absolutePath}")
                        }
                    } else {
                        Log.w("ProductsRedActivity", "Old image file not found for deletion: ${oldFile.absolutePath}")
                    }
                } else {
                    Log.w("ProductsRedActivity", "Skipping cleanup, URI is not an internal file URI: $oldUriString")
                }
            } catch (e: Exception) {
                Log.e("ProductsRedActivity", "Error cleaning up old image: $oldUriString", e)
            }
        }
    }

} // Конец класса