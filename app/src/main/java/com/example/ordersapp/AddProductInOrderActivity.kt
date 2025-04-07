package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityAddProductInOrderBinding

class AddProductInOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductInOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductInOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}