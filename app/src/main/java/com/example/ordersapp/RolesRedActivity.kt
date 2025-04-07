package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityProductsRedBinding
import com.example.ordersapp.databinding.ActivityRolesRedBinding

class RolesRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRolesRedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRolesRedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}