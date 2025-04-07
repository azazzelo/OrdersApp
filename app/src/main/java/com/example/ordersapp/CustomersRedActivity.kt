package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityCustomersRedBinding

class CustomersRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomersRedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}