package com.example.ordersapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ordersapp.databinding.ActivityRolesRedBinding // Проверьте путь
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.RolesDao
import com.example.ordersapp.db.RolesEntity
import kotlinx.coroutines.launch

class RolesRedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRolesRedBinding
    private lateinit var rolesDao: RolesDao
    private var currentRole: RolesEntity? = null // Для хранения данных при редактировании

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRolesRedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}