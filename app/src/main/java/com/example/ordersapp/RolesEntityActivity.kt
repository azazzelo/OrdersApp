package com.example.ordersapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.adapters.RolesAdapter // Проверьте путь
import com.example.ordersapp.databinding.ActivityRolesEntityBinding // Проверьте путь
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.RolesDao
import kotlinx.coroutines.launch

class RolesEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRolesEntityBinding
    private lateinit var adapter: RolesAdapter // Используем упрощенный адаптер
    private lateinit var rolesDao: RolesDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRolesEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверка роли Директора все еще имеет смысл,
        // чтобы другие роли не могли даже просмотреть этот список,
        // если доступ к кнопке в TablesActivity не сработает.
        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val roleId = sharedPrefs.getInt("roleId", -1)
        if (roleId != 1) {
            Log.w("RolesEntityActivity", "Access denied for roleId: $roleId")
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rolesDao = AppDatabase.getDatabase(this).rolesDao()
        setupRecyclerView()
        loadRoles()
    }

    private fun setupRecyclerView() {
        // Используем адаптер без обработчиков кликов
        adapter = RolesAdapter()
        binding.rcRoles.adapter = adapter
        binding.rcRoles.layoutManager = LinearLayoutManager(this)
        binding.rcRoles.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadRoles() {
        lifecycleScope.launch {
            try {
                val rolesList = rolesDao.getAllRoles()
                adapter.submitList(rolesList)
                Log.d("RolesEntityActivity", "Roles loaded: ${rolesList.size} items.")
            } catch (e: Exception) {
                Log.e("RolesEntityActivity", "Error loading roles", e)
                Toast.makeText(this@RolesEntityActivity, "Error loading roles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Убраны методы deleteRole и обработчик результата roleActivityResultLauncher
}