package com.example.ordersapp.adapters // Или ваш пакет для адаптеров

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog // Импортируем AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ordersapp.R // Импорт R для доступа к ресурсам drawable
import com.example.ordersapp.databinding.ProductInOrderItemBinding // Убедитесь, что путь верный и название совпадает с вашим XML
import com.example.ordersapp.db.ProductsEntity
import kotlinx.coroutines.* // Для корутин проверки файла
import java.io.File // Для проверки файла
import java.util.Locale

// Data class для удобного хранения информации о товаре в заказе
data class ProductInOrderDisplay(
    val product: ProductsEntity, // Полная информация о товаре
    var quantityInOrder: Int    // Количество именно в этом заказе
)

class OrderProductAdapter(
    private val onProductDelete: (ProductInOrderDisplay) -> Unit,
    private val onQuantityChange: (ProductInOrderDisplay, Int) -> Unit // Для изменения количества (если нужно)
) : RecyclerView.Adapter<OrderProductAdapter.OrderProductViewHolder>() {

    private var productList = mutableListOf<ProductInOrderDisplay>()
    // Scope для корутин внутри адаптера (для проверки файлов)
    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())


    inner class OrderProductViewHolder(val binding: ProductInOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var imageLoadingJob: Job? = null // Job для отмены загрузки

        fun bind(item: ProductInOrderDisplay) {
            // Используем ID из ТВОЕГО product_in_order_item.xml
            binding.tvProductIdValue1.text = item.product.idProduct.toString()
            binding.tvCostValue1.text = String.format(Locale.ROOT, "%.2f", item.product.cost)
            binding.tvQuantityValue1.text = item.quantityInOrder.toString()

            // Делаем TextView со значениями видимыми
            binding.tvProductIdValue1.visibility = View.VISIBLE
            binding.tvCostValue1.visibility = View.VISIBLE
            binding.tvQuantityValue1.visibility = View.VISIBLE

            // --- Загрузка изображения ---
            imageLoadingJob?.cancel() // Отменяем предыдущую загрузку для этого ViewHolder'а
            binding.imageView.visibility = View.GONE // Сначала скрываем
            Glide.with(binding.imageView.context).clear(binding.imageView) // Очищаем предыдущее

            item.product.photoUri?.let { uriString ->
                imageLoadingJob = adapterScope.launch { // Запускаем корутину для проверки и загрузки
                    var fileExists = false
                    var displayUri: Uri? = null
                    try {
                        displayUri = Uri.parse(uriString)
                        // Проверяем существование файла в фоновом потоке IO
                        fileExists = withContext(Dispatchers.IO) {
                            try {
                                displayUri.path?.let { File(it).exists() } ?: false
                            } catch (e: Exception) {
                                false // Ошибка при доступе к пути
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("OrderProductAdapter", "Error parsing URI $uriString", e)
                    }

                    // Если файл существует, загружаем через Glide
                    if (isActive && fileExists && displayUri != null) { // Проверяем isActive на случай отмены
                        try {
                            Glide.with(binding.imageView.context)
                                .load(displayUri) // Загружаем URI файла
                                .placeholder(R.drawable.ic_launcher_background) // ЗАМЕНИТЕ
                                .error(R.drawable.ic_launcher_foreground)     // ЗАМЕНИТЕ
                                .into(binding.imageView)
                            binding.imageView.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            Log.e("OrderProductAdapter", "Error loading image with Glide $uriString", e)
                            binding.imageView.visibility = View.GONE
                        }
                    } else if(isActive) {
                        // Файл не найден или URI битый
                        Log.w("OrderProductAdapter", "Image file not found or URI invalid: $uriString")
                        binding.imageView.visibility = View.GONE
                    } else {
                        Log.d("OrderProductAdapter", "Coroutine cancelled before loading image: $uriString")
                    }
                }
            } // Если photoUri == null, ImageView остается GONE

            // Удаление по долгому нажатию на весь элемент
            binding.root.setOnLongClickListener {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Удалить товар?")
                    .setMessage("Удалить '${item.product.name}' (${item.quantityInOrder} шт.) из заказа?")
                    .setPositiveButton("Удалить") { _, _ ->
                        onProductDelete(item)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                true
            }
        }

        fun cancelJobs() {
            imageLoadingJob?.cancel()
        }
    }

    // Отменяем корутины, когда View отсоединяется или переиспользуется
    override fun onViewRecycled(holder: OrderProductViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelJobs()
    }

    override fun onViewDetachedFromWindow(holder: OrderProductViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.cancelJobs()
    }

    // --- Остальные методы адаптера без изменений ---
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderProductViewHolder {
        val binding = ProductInOrderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    fun submitList(newItems: List<ProductInOrderDisplay>) {
        productList.clear()
        productList.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: ProductInOrderDisplay) {
        val existingItem = productList.find { it.product.idProduct == item.product.idProduct }
        if (existingItem != null) {
            existingItem.quantityInOrder += item.quantityInOrder
            notifyItemChanged(productList.indexOf(existingItem))
        } else {
            productList.add(item)
            notifyItemInserted(productList.size - 1)
        }
    }

    fun removeItem(item: ProductInOrderDisplay) {
        val index = productList.indexOf(item)
        if (index != -1) {
            productList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getCurrentList(): List<ProductInOrderDisplay> {
        return productList.toList()
    }

    // Отменяем все корутины адаптера при его уничтожении (если нужно)
    fun cancelAllJobs() {
        adapterScope.cancel()
    }
}