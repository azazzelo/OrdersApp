package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityCustomersEntityBinding

class CustomersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomersEntityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}