package com.example.ordersapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ordersapp.databinding.ActivityMainBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.RolesEntity
import com.example.ordersapp.db.UsersEntity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        val usersDao = db.usersDao()
        val rolesDao = db.rolesDao()


        lifecycleScope.launch {
            if (rolesDao.getAllRoles().isEmpty()) {
                // Добавляем роли
                val roles = listOf(
                    RolesEntity(1, "Директор"),
                    RolesEntity(2, "Диспетчер"),
                    RolesEntity(3, "Бухгалтер")
                )
                roles.forEach { rolesDao.insert(it) }

                // Добавляем пользователей
                val users = listOf(
                    UsersEntity(1, 1, "schwein", "112233"),
                    UsersEntity(2, 2, "d", "123"),
                    UsersEntity(3, 3, "b", "321")
                )
                users.forEach { usersDao.insert(it) }
            }
        }



        binding.btLogin.setOnClickListener {
            val login = binding.edTextLogin.text.toString()
            val password = binding.edTextPassword.text.toString()

            lifecycleScope.launch {
                val user = usersDao.getUserByCredentials(login, password)
                if (user != null) {
                    // Сохраняем роль пользователя
                    val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
                    sharedPrefs.edit().putInt("roleId", user.idRole).apply()

                    // Переход к TablesActivity
                    startActivity(Intent(this@MainActivity, TablesActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Неверный логин или пароль",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}