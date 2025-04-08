package com.example.ordersapp.db // Или ваш пакет для адаптеров

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ordersapp.databinding.OrdersListItemBinding // Убедитесь, что путь верный
import kotlinx.coroutines.* // Импортируем все из корутин
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(
    private val onItemClick: (OrdersEntity) -> Unit,
    private val onItemDelete: (OrdersEntity) -> Unit,
    private val getCustomerName: suspend (Int) -> String? // Суспенд-функция для получения имени
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private var orders = emptyList<OrdersEntity>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    inner class OrderViewHolder(val binding: OrdersListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Создаем кастомный CoroutineScope для этого ViewHolder'а
        // Он будет использовать Main диспетчер и будет отменяться при отсоединении View
        private val viewHolderScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

        fun bind(order: OrdersEntity) {
            // Привязываем данные к элементам ViewBinding
            binding.tvItemOrderIdValue.text = order.idOrder.toString()
            binding.tvItemRegistrationDateValue.text = dateFormat.format(Date(order.dateRegistration))
            binding.tvItemDeliveryDateValue.text = dateFormat.format(Date(order.dateDelivery))
            binding.tvItemFinallyCostValue.text = String.format(Locale.ROOT, "%.2f", order.finallyCost)

            // Показываем TextView со значениями
            binding.tvItemOrderIdValue.visibility = android.view.View.VISIBLE
            binding.tvItemRegistrationDateValue.visibility = android.view.View.VISIBLE
            binding.tvItemDeliveryDateValue.visibility = android.view.View.VISIBLE
            binding.tvItemFinallyCostValue.visibility = android.view.View.VISIBLE
            binding.tvItemCustomerIdValue.visibility = android.view.View.VISIBLE

            // Устанавливаем текст по умолчанию для имени клиента
            binding.tvItemCustomerIdValue.text = "Клиент ID: ${order.idCustomer}"

            // Запускаем корутину в scope ViewHolder'а для загрузки имени клиента
            viewHolderScope.launch {
                try {
                    // Вызываем suspend-лямбду getCustomerName ЗДЕСЬ (внутри корутины)
                    val customerName = getCustomerName(order.idCustomer)

                    // Обновляем UI (мы уже в Dispatchers.Main.immediate)
                    if (customerName != null) {
                        binding.tvItemCustomerIdValue.text = customerName // Показываем имя клиента
                    } else {
                        binding.tvItemCustomerIdValue.text = "Клиент ID: ${order.idCustomer} (не найден)"
                    }
                    Log.d("OrdersAdapter", "Customer name loaded for ID ${order.idCustomer}: $customerName")

                } catch (e: CancellationException) {
                    // Корутина была отменена (например, view переиспользовалась) - это нормально
                    Log.d("OrdersAdapter", "Customer name loading cancelled for ID ${order.idCustomer}")
                } catch (e: Exception) {
                    // Другая ошибка при загрузке
                    binding.tvItemCustomerIdValue.text = "Клиент ID: ${order.idCustomer} (ошибка)"
                    Log.e("OrdersAdapter", "Error getting customer name for ID ${order.idCustomer}", e)
                }
            }

            // Устанавливаем слушатели кликов
            binding.root.setOnClickListener {
                onItemClick(order)
            }
            binding.root.setOnLongClickListener {
                onItemDelete(order)
                true
            }
        }

        // Метод для отмены всех корутин, запущенных в этом ViewHolder'е
        fun cancelJobs() {
            viewHolderScope.cancel() // Отменяем scope и все его дочерние корутины
            Log.d("OrdersAdapter", "Jobs cancelled for ViewHolder: $adapterPosition")
        }
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

    // Отменяем корутины, когда View отсоединяется от окна
    override fun onViewDetachedFromWindow(holder: OrderViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.cancelJobs()
    }

    // Отменяем корутины, когда View переиспользуется
    override fun onViewRecycled(holder: OrderViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelJobs()
    }


    override fun getItemCount(): Int {
        return orders.size
    }

    fun submitList(newList: List<OrdersEntity>) {
        orders = newList
        notifyDataSetChanged()
    }
}