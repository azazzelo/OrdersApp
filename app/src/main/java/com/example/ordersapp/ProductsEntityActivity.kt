package com.example.ordersapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.databinding.ActivityProductsEntityBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.ProductsAdapter
import com.example.ordersapp.db.ProductsDao
import com.example.ordersapp.db.ProductsEntity
import kotlinx.coroutines.launch

class ProductsEntityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductsEntityBinding
    private lateinit var adapter: ProductsAdapter
    private lateinit var productsDao: ProductsDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productsDao = AppDatabase.getDatabase(this).productsDao()
        setupRecyclerView()

        binding.btnAddProductt.setOnClickListener {
            startActivityForResult(
                Intent(this, ProductsRedActivity::class.java),
                REQUEST_ADD_PRODUCT
            )
        }

        loadProducts()
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onItemClick = { product -> onProductClick(product) },
            onItemDelete = { product -> deleteProduct(product) }
        )
        binding.rcProducts.adapter = adapter
        binding.rcProducts.layoutManager = LinearLayoutManager(this)
        binding.rcProducts.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            val products = productsDao.getAllProducts()
            adapter.submitList(products)
        }
    }

    private fun onProductClick(product: ProductsEntity) {
        startActivityForResult(
            Intent(this, ProductsRedActivity::class.java).apply {
                putExtra("product_id", product.idProduct)
            },
            REQUEST_EDIT_PRODUCT
        )
    }

    private fun deleteProduct(product: ProductsEntity) {
        AlertDialog.Builder(this)
            .setTitle("Удаление товара")
            .setMessage("Удалить ${product.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    productsDao.delete(product)
                    loadProducts()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    companion object {
        private const val REQUEST_ADD_PRODUCT = 1
        private const val REQUEST_EDIT_PRODUCT = 2
    }
}