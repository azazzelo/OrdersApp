package com.example.ordersapp.adapters // Или ваш пакет

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ordersapp.databinding.RolesListItemBinding // Убедитесь, что путь и имя верны
import com.example.ordersapp.db.RolesEntity

// Адаптер только для отображения, без кликов
class RolesAdapter : RecyclerView.Adapter<RolesAdapter.RoleViewHolder>() {

    private var roles = emptyList<RolesEntity>()

    inner class RoleViewHolder(val binding: RolesListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(role: RolesEntity) {
            // Используем ID из roles_list_item.xml
            binding.tvItemRoleIdValue.text = role.idRole.toString()
            binding.tvItemRoleNameValue.text = role.name
            // Убраны обработчики кликов
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val binding = RolesListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.bind(roles[position])
    }

    override fun getItemCount(): Int = roles.size

    fun submitList(newList: List<RolesEntity>) {
        roles = newList
        notifyDataSetChanged()
    }
}