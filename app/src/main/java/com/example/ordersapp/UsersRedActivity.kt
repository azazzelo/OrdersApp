package com.example.ordersapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ordersapp.databinding.ActivityProductsRedBinding
import com.example.ordersapp.databinding.ActivityUsersRedBinding

class UsersRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersRedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}