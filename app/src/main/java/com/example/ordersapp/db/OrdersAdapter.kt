package com.example.ordersapp.db // Или ваш пакет для адаптеров

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ordersapp.databinding.OrdersListItemBinding // Убедитесь, что путь верный
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View // Импорт View

class OrdersAdapter(
    // Убрали getCustomerName из конструктора
    private val onItemClick: (OrdersEntity) -> Unit,
    private val onItemDelete: (OrdersEntity) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private var orders = emptyList<OrdersEntity>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val priceFormat = "%.2f" // Форматтер для цен

    inner class OrderViewHolder(val binding: OrdersListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Убрали viewHolderScope и customerNameJob

        fun bind(order: OrdersEntity) {
            binding.tvItemOrderIdValue.text = order.idOrder.toString()
            binding.tvItemRegistrationDateValue.text = dateFormat.format(Date(order.dateRegistration))
            binding.tvItemDeliveryDateValue.text = dateFormat.format(Date(order.dateDelivery))
            binding.tvItemFinallyCostValue.text = String.format(Locale.US, priceFormat, order.finallyCost) // Используем Locale.US для точки

            // --- ВСЕГДА ПОКАЗЫВАЕМ ТОЛЬКО ID КЛИЕНТА ---
            binding.tvItemCustomerIdValue.text = order.idCustomer.toString() // Просто ID
            // Убедимся, что соответствующие метки и значения видимы
            // (зависит от вашего orders_list_item.xml)
            binding.tvItemOrderId.visibility = View.VISIBLE
            binding.tvItemOrderIdValue.visibility = View.VISIBLE
            binding.tvItemCustomerId.visibility = View.VISIBLE // Метка ID клиента
            binding.tvItemCustomerIdValue.visibility = View.VISIBLE // Значение ID клиента
            binding.tvItemRegistrationDate.visibility = View.VISIBLE
            binding.tvItemRegistrationDateValue.visibility = View.VISIBLE
            binding.tvItemDeliveryDate.visibility = View.VISIBLE
            binding.tvItemDeliveryDateValue.visibility = View.VISIBLE
            binding.tvItemFinallyCost.visibility = View.VISIBLE
            binding.tvItemFinallyCostValue.visibility = View.VISIBLE


            // Убрали корутину для загрузки имени

            binding.root.setOnClickListener { onItemClick(order) }
            binding.root.setOnLongClickListener {
                onItemDelete(order)
                true
            }
        }

        // Убрали cancelJobs()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = OrdersListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    // Убрали onViewDetachedFromWindow и onViewRecycled, так как нет Job'ов для отмены

    override fun getItemCount(): Int {
        return orders.size
    }

    fun submitList(newList: List<OrdersEntity>) {
        orders = newList
        notifyDataSetChanged()
    }
}