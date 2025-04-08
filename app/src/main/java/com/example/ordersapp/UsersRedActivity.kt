package com.example.ordersapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
// ИСПОЛЬЗУЕМ ТВОЙ BINDING КЛАСС
import com.example.ordersapp.databinding.ActivityUsersRedBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.RolesDao
import com.example.ordersapp.db.RolesEntity
import com.example.ordersapp.db.UsersDao
import com.example.ordersapp.db.UsersEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsersRedActivity : AppCompatActivity() {

    // ИСПОЛЬЗУЕМ ТВОЙ BINDING КЛАСС
    private lateinit var binding: ActivityUsersRedBinding
    private lateinit var usersDao: UsersDao
    private lateinit var rolesDao: RolesDao
    private var currentUser: UsersEntity? = null
    private var rolesList: List<RolesEntity> = emptyList()
    private var selectedRoleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ИСПОЛЬЗУЕМ ТВОЙ BINDING КЛАСС
        binding = ActivityUsersRedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверка роли Директора (оставляем)
        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val userRoleIdPref = sharedPrefs.getInt("roleId", -1)
        if (userRoleIdPref != 1) {
            Log.w("UsersRedActivity", "Access denied for roleId: $userRoleIdPref")
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        usersDao = db.usersDao()
        rolesDao = db.rolesDao()

        loadRolesForSpinner()

        val userId = intent.getIntExtra("user_id", -1)
        if (userId != -1) {
            Log.d("UsersRedActivity", "Editing user with ID: $userId")
            loadUserData(userId)
            if (userId == 1) {
                binding.spinnerUserRole.isEnabled = false // Запрет смены роли админу
            }
        } else {
            Log.d("UsersRedActivity", "Adding new user.")
            // Используем ID заголовка из твоего макета
            binding.textViewUserTitle.text = "Add User" // Или другая строка
            // Используем ID поля пароля из твоего макета
            binding.edTextUserPassword.hint = "Password (required)"
        }

        setupRoleSpinner()

        // Используем ID кнопки сохранения из твоего макета (FloatingActionButton)
        binding.btnSaveUser.setOnClickListener {
            saveUser()
        }
    }

    private fun loadRolesForSpinner() {
        lifecycleScope.launch {
            try {
                rolesList = withContext(Dispatchers.IO) { rolesDao.getAllRoles() }
                if (::binding.isInitialized && binding.spinnerUserRole.adapter != null) { // Проверяем инициализацию binding
                    updateSpinnerAdapter()
                }
                Log.d("UsersRedActivity", "Roles loaded for spinner: ${rolesList.size} items.")
                currentUser?.let { selectRoleInSpinner(it.idRole) }
            } catch (e: Exception) {
                Log.e("UsersRedActivity", "Error loading roles for spinner", e)
                Toast.makeText(this@UsersRedActivity, "Error loading roles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRoleSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Используем ID спиннера из твоего макета
        binding.spinnerUserRole.adapter = adapter

        binding.spinnerUserRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < rolesList.size) {
                    selectedRoleId = rolesList[position].idRole
                    Log.d("UsersRedActivity", "Role selected: ID=$selectedRoleId, Name=${rolesList[position].name}")
                } else {
                    selectedRoleId = -1
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRoleId = -1
            }
        }
        if (rolesList.isNotEmpty()) {
            updateSpinnerAdapter()
        }
    }

    private fun updateSpinnerAdapter() {
        val roleNames = rolesList.map { it.name }
        // Используем ID спиннера из твоего макета
        val adapter = binding.spinnerUserRole.adapter as ArrayAdapter<String>
        adapter.clear()
        adapter.addAll(roleNames)
        adapter.notifyDataSetChanged()
        Log.d("UsersRedActivity", "Spinner adapter updated.")
    }

    private fun loadUserData(userId: Int) {
        lifecycleScope.launch {
            try {
                currentUser = withContext(Dispatchers.IO) { usersDao.getUserById(userId) }
                currentUser?.let { user ->
                    // Используем ID полей из твоего макета
                    binding.edTextUserLogin.setText(user.login)
                    binding.textViewUserTitle.text = "Edit User: ${user.login}" // Заголовок
                    // Пароль не загружаем
                    binding.edTextUserPassword.hint = "Password (leave blank to keep old)" // Обновляем подсказку
                    selectRoleInSpinner(user.idRole)
                    Log.i("UsersRedActivity", "User data loaded for ID: $userId")
                } ?: run {
                    Log.w("UsersRedActivity", "User with ID $userId not found.")
                    Toast.makeText(this@UsersRedActivity, "User not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("UsersRedActivity", "Error loading user $userId", e)
                Toast.makeText(this@UsersRedActivity, "Error loading user data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun selectRoleInSpinner(roleIdToSelect: Int) {
        val position = rolesList.indexOfFirst { it.idRole == roleIdToSelect }
        if (position != -1) {
            // Используем ID спиннера из твоего макета
            binding.spinnerUserRole.setSelection(position)
            Log.d("UsersRedActivity", "Role with ID $roleIdToSelect selected in spinner at position $position.")
        } else {
            Log.w("UsersRedActivity", "Role with ID $roleIdToSelect not found in rolesList for spinner selection.")
        }
    }

    private fun saveUser() {
        // Используем ID полей из твоего макета
        val login = binding.edTextUserLogin.text.toString().trim()
        val password = binding.edTextUserPassword.text.toString()

        if (login.isEmpty()) {
            binding.edTextUserLogin.error = "Login cannot be empty"
            return
        }
        if (currentUser == null && password.isEmpty()) {
            binding.edTextUserPassword.error = "Password is required for new user"
            return
        }
        if (selectedRoleId == -1 || rolesList.find { it.idRole == selectedRoleId } == null) {
            Toast.makeText(this, "Please select a valid role", Toast.LENGTH_SHORT).show()
            return
        }
        if(currentUser?.idUser == 1 && selectedRoleId != 1){
            Toast.makeText(this, "Cannot change role for the main administrator", Toast.LENGTH_SHORT).show()
            return
        }

        val userToSave: UsersEntity
        if (currentUser == null) {
            Log.d("UsersRedActivity", "Creating new user object: login=$login, roleId=$selectedRoleId")
            userToSave = UsersEntity(0, selectedRoleId, login, password)
        } else {
            Log.d("UsersRedActivity", "Updating user object ID: ${currentUser!!.idUser}, login=$login, roleId=$selectedRoleId")
            val passwordToSave = if (password.isNotEmpty()) password else currentUser!!.password
            userToSave = currentUser!!.copy(
                idRole = selectedRoleId,
                login = login,
                password = passwordToSave
            )
        }

        lifecycleScope.launch {
            try {
                if (currentUser == null) {
                    usersDao.insert(userToSave)
                    Log.i("UsersRedActivity", "New user inserted.")
                    Toast.makeText(this@UsersRedActivity, "User added", Toast.LENGTH_SHORT).show()
                } else {
                    usersDao.updateUser(userToSave)
                    Log.i("UsersRedActivity", "User ${userToSave.idUser} updated.")
                    Toast.makeText(this@UsersRedActivity, "User updated", Toast.LENGTH_SHORT).show()
                }
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Log.e("UsersRedActivity", "Error saving user", e)
                Toast.makeText(this@UsersRedActivity, "Error saving user", Toast.LENGTH_SHORT).show()
            }
        }
    }
}