package com.example.ordersapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ordersapp.adapters.UsersAdapter // Проверьте путь
import com.example.ordersapp.databinding.ActivityUsersEntityBinding // Проверьте путь
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.RolesDao
import com.example.ordersapp.db.UsersDao
import com.example.ordersapp.db.UsersEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsersEntityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersEntityBinding
    private lateinit var adapter: UsersAdapter
    private lateinit var usersDao: UsersDao
    private lateinit var rolesDao: RolesDao
    private var rolesMap: Map<Int, String> = emptyMap() // Карта ID роли -> Имя роли
    private var currentUserId: Int = -1 // ID текущего авторизованного пользователя (Директора)

    // Лаунчер для результата из UsersRedActivity
    private val userActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadUsersAndRoles() // Перезагружаем все данные
            Log.d("UsersEntityActivity", "User list reloaded after result OK.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersEntityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверка роли Директора
        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val roleId = sharedPrefs.getInt("roleId", -1)
        // Получаем также ID текущего пользователя, чтобы нельзя было удалить самого себя
        // Предполагаем, что ID пользователя сохраняется при логине или доступен иначе
        // Если ID нет в SharedPreferences, его нужно добавить при логине в MainActivity
        // currentUserId = sharedPrefs.getInt("userId", -1) // Пример
        // !!! ВАЖНО: Убедитесь, что 'userId' сохраняется в SharedPreferences в MainActivity !!!
        // Если его там нет, временно закомментируйте проверку на currentUserId при удалении

        if (roleId != 1) {
            Log.w("UsersEntityActivity", "Access denied for roleId: $roleId")
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        usersDao = db.usersDao()
        rolesDao = db.rolesDao()

        setupRecyclerView() // Настраиваем RecyclerView ДО загрузки данных

        binding.btnAddUser.setOnClickListener {
            val intent = Intent(this, UsersRedActivity::class.java)
            userActivityResultLauncher.launch(intent)
            Log.d("UsersEntityActivity", "Starting UsersRedActivity for add.")
        }

        loadUsersAndRoles() // Загружаем роли и пользователей
    }

    private fun setupRecyclerView() {
        // Создаем адаптер с пустой картой ролей сначала
        adapter = UsersAdapter(
            rolesMap, // Передаем карту ролей
            onItemClick = { user ->
                // Клик - редактирование
                Log.d("UsersEntityActivity", "Clicked on user: ${user.idUser}")
                val intent = Intent(this, UsersRedActivity::class.java).apply {
                    putExtra("user_id", user.idUser) // Передаем ID пользователя
                }
                userActivityResultLauncher.launch(intent)
            },
            onItemDelete = { user ->
                // Долгое нажатие - удаление
                Log.d("UsersEntityActivity", "Long clicked on user: ${user.idUser}")
                deleteUser(user)
            }
        )
        binding.rcUsers.adapter = adapter
        binding.rcUsers.layoutManager = LinearLayoutManager(this)
        binding.rcUsers.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    // Загружает сначала роли, потом пользователей
    private fun loadUsersAndRoles() {
        lifecycleScope.launch {
            try {
                // 1. Загружаем роли и создаем карту
                val rolesList = withContext(Dispatchers.IO) { rolesDao.getAllRoles() }
                rolesMap = rolesList.associateBy({ it.idRole }, { it.name })
                Log.d("UsersEntityActivity", "Roles map created: ${rolesMap.size} items.")

                // Обновляем карту в адаптере (если он уже создан)
                // (Лучше было бы передать карту через конструктор, но так тоже сработает)
                if (::adapter.isInitialized) {
                    adapter = UsersAdapter(rolesMap, adapter.onItemClick, adapter.onItemDelete) // Пересоздаем адаптер с новой картой
                    binding.rcUsers.adapter = adapter // Устанавливаем новый адаптер
                } else {
                    setupRecyclerView() // Если адаптер не был создан, создаем его сейчас с картой
                }


                // 2. Загружаем пользователей
                val usersList = withContext(Dispatchers.IO) { usersDao.getAllUsers() }
                adapter.submitList(usersList) // Обновляем список в адаптере
                Log.d("UsersEntityActivity", "Users loaded: ${usersList.size} items.")

            } catch (e: Exception) {
                Log.e("UsersEntityActivity", "Error loading roles or users", e)
                Toast.makeText(this@UsersEntityActivity, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteUser(user: UsersEntity) {
        // Запрет удаления самого себя (если currentUserId доступен)
        // if (currentUserId != -1 && user.idUser == currentUserId) {
        //     Toast.makeText(this, "You cannot delete your own account", Toast.LENGTH_SHORT).show()
        //     Log.w("UsersEntityActivity", "Attempted to delete self (User ID: $currentUserId)")
        //     return
        // }

        // Запрет удаления первого пользователя (Директора)
        if (user.idUser == 1) {
            Toast.makeText(this, "Cannot delete the main administrator account (ID 1)", Toast.LENGTH_SHORT).show()
            Log.w("UsersEntityActivity", "Attempted to delete main admin (User ID: 1)")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete user '${user.login}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        usersDao.deleteUser(user)
                        Log.i("UsersEntityActivity", "User ${user.idUser} deleted successfully.")
                        Toast.makeText(this@UsersEntityActivity, "User deleted", Toast.LENGTH_SHORT).show()
                        loadUsersAndRoles() // Перезагружаем список
                    } catch (e: Exception) {
                        // Обработка возможных ошибок БД (хотя у пользователя нет внешних ключей, влияющих на удаление)
                        Log.e("UsersEntityActivity", "Error deleting user ${user.idUser}", e)
                        Toast.makeText(this@UsersEntityActivity, "Error deleting user", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}