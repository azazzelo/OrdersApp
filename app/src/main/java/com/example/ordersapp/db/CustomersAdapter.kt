package com.example.ordersapp.db // Или ваш пакет для адаптеров

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ordersapp.databinding.CustomersListItemBinding // Убедитесь, что путь верный

class CustomersAdapter(
    private val onItemClick: (CustomersEntity) -> Unit,
    private val onItemDelete: (CustomersEntity) -> Unit
) : RecyclerView.Adapter<CustomersAdapter.CustomerViewHolder>() {

    private var customers = emptyList<CustomersEntity>()

    inner class CustomerViewHolder(val binding: CustomersListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomersEntity) {
            // Привязываем данные к элементам ViewBinding
            // Важно: В вашем customers_list_item.xml ID TextView для значений
            // имеют суффикс Value (tvItemCustomerIdValue, tvItemCustomerNameValue и т.д.)
            binding.tvItemCustomerIdValue.text = customer.idCustomer.toString()
            binding.tvItemCustomerNameValue.text = customer.name
            binding.tvItemCustomerPhoneValue.text = customer.phone
            binding.tvItemCustomerEmailValue.text = customer.contactPersonEmail

            // Показываем TextView со значениями (если они были невидимы по умолчанию)
            binding.tvItemCustomerIdValue.visibility = android.view.View.VISIBLE
            binding.tvItemCustomerNameValue.visibility = android.view.View.VISIBLE
            binding.tvItemCustomerPhoneValue.visibility = android.view.View.VISIBLE
            binding.tvItemCustomerEmailValue.visibility = android.view.View.VISIBLE


            // Устанавливаем слушатели кликов
            binding.root.setOnClickListener {
                onItemClick(customer)
            }
            binding.root.setOnLongClickListener {
                onItemDelete(customer)
                true // Возвращаем true, чтобы показать, что событие обработано
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = CustomersListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customers[position])
    }

    override fun getItemCount(): Int {
        return customers.size
    }

    // Метод для обновления списка данных
    fun submitList(newList: List<CustomersEntity>) {
        customers = newList
        notifyDataSetChanged() // Уведомляем адаптер об изменениях
    }
}