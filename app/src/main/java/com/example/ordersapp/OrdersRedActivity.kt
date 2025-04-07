package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityOrdersRedBinding

class OrdersRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersRedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}