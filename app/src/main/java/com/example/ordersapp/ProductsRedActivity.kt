package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityProductsRedBinding

class ProductsRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductsRedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsRedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}