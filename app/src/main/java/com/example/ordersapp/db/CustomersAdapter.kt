package com.example.ordersapp.db // Или ваш пакет для адаптеров

import android.view.LayoutInflater
import android.view.View // Импорт View
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
            binding.tvItemCustomerIdValue.text = customer.idCustomer.toString()
            binding.tvItemCustomerNameValue.text = customer.name
            binding.tvItemCustomerPhoneValue.text = customer.phone
            binding.tvItemCustomerEmailValue.text = customer.contactPersonEmail

            // --- ОТОБРАЖАЕМ АДРЕС ---
            if (!customer.address.isNullOrEmpty()) {
                binding.tvItemCustomerAddressValue.text = customer.address
                binding.layoutCustomerAddress.visibility = View.VISIBLE // Показываем блок с адресом
            } else {
                binding.layoutCustomerAddress.visibility = View.GONE // Скрываем блок, если адреса нет
            }
            // ----------------------

            // Показываем остальные TextView со значениями
            binding.tvItemCustomerIdValue.visibility = View.VISIBLE
            binding.tvItemCustomerNameValue.visibility = View.VISIBLE
            binding.tvItemCustomerPhoneValue.visibility = View.VISIBLE
            binding.tvItemCustomerEmailValue.visibility = View.VISIBLE

            binding.root.setOnClickListener { onItemClick(customer) }
            binding.root.setOnLongClickListener {
                onItemDelete(customer)
                true
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

    override fun getItemCount(): Int = customers.size

    fun submitList(newList: List<CustomersEntity>) {
        customers = newList
        notifyDataSetChanged()
    }
}