package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityProductsRedBinding
import com.example.ordersapp.databinding.ActivityRolesEntityBinding

class RolesEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRolesEntityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRolesEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}