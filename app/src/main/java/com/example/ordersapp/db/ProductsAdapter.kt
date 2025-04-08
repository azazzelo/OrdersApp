package com.example.ordersapp.db

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout // Импортируем ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Импортируем Glide
import com.example.ordersapp.R
import com.example.ordersapp.databinding.ProductsListItemBinding

class ProductsAdapter(
    private val onItemClick: (ProductsEntity) -> Unit,
    private val onItemDelete: (ProductsEntity) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    private var products = emptyList<ProductsEntity>()

    inner class ProductViewHolder(val binding: ProductsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductsEntity) {
            // Отображаем текстовые данные
            binding.tvProductIdValue.text = product.idProduct.toString()
            binding.tvNameValue.text = product.name
            binding.tvCostValue.text = product.cost.toString()
            binding.tvQuantityValue.text = product.quantity.toString()

            // Используем Glide для загрузки изображения
            product.photoUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    Glide.with(binding.imageView.context) // Используем контекст ImageView
                        .load(uri) // Загружаем URI
                        .placeholder(R.drawable.ic_launcher_background) // Опционально: картинка пока грузится
                        .error(R.drawable.ic_launcher_foreground) // Опционально: картинка если ошибка загрузки
                        .into(binding.imageView) // В какой ImageView загружать
                    binding.imageView.visibility = View.VISIBLE

                    // Адаптируем layout для картинки
                    val params = binding.textContainer.layoutParams as ConstraintLayout.LayoutParams
                    params.endToStart = binding.imageView.id
                    binding.textContainer.layoutParams = params

                } catch (e: Exception) {
                    // Ошибка парсинга URI или другая проблема перед Glide
                    Log.e("ProductsAdapter", "Error parsing URI or setting up Glide for: $uriString", e)
                    binding.imageView.visibility = View.GONE
                    // Адаптируем layout для отсутствия картинки
                    val params = binding.textContainer.layoutParams as ConstraintLayout.LayoutParams
                    params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    binding.textContainer.layoutParams = params
                }
            } ?: run {
                // Если photoUri == null
                Glide.with(binding.imageView.context).clear(binding.imageView) // Очищаем ImageView Glide'ом
                binding.imageView.visibility = View.GONE
                // Адаптируем layout для отсутствия картинки
                val params = binding.textContainer.layoutParams as ConstraintLayout.LayoutParams
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                binding.textContainer.layoutParams = params
            }

            // Обработчики нажатий
            binding.root.setOnClickListener { onItemClick(product) }
            binding.root.setOnLongClickListener {
                onItemDelete(product)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductsListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun submitList(newList: List<ProductsEntity>) {
        products = newList
        notifyDataSetChanged()
    }
}