package com.example.ordersapp.adapters // Или ваш пакет

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ordersapp.databinding.UsersListItemBinding // Убедитесь, что путь и имя верны
import com.example.ordersapp.db.RolesEntity // Нужна для передачи списка ролей
import com.example.ordersapp.db.UsersEntity
import kotlinx.coroutines.* // Для корутин не нужен, т.к. роли передаются

class UsersAdapter(
    private val rolesMap: Map<Int, String>, // Карта для быстрого получения имени роли по ID
    internal val onItemClick: (UsersEntity) -> Unit,
    internal val onItemDelete: (UsersEntity) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private var users = emptyList<UsersEntity>()

    inner class UserViewHolder(val binding: UsersListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UsersEntity) {
            binding.tvItemUserIdValue.text = user.idUser.toString()
            binding.tvItemUserLoginValue.text = user.login

            // Получаем имя роли из переданной карты
            val roleName = rolesMap[user.idRole] ?: "Unknown Role (ID: ${user.idRole})"
            binding.tvItemUserRoleValue.text = roleName

            // Слушатели
            binding.root.setOnClickListener { onItemClick(user) }
            binding.root.setOnLongClickListener {
                onItemDelete(user)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UsersListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    // Обновляем список пользователей
    fun submitList(newList: List<UsersEntity>) {
        users = newList
        notifyDataSetChanged()
    }
}