package com.example.ordersapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ordersapp.databinding.ActivityUsersEntityBinding

class UsersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersEntityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}