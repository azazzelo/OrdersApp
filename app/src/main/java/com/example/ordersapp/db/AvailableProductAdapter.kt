package com.example.ordersapp.adapters // Или ваш пакет

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ordersapp.databinding.AvailableProductListItemBinding // Проверьте путь
import com.example.ordersapp.db.ProductsEntity
import java.util.Locale

class AvailableProductAdapter(
    private val onProductClick: (ProductsEntity) -> Unit
) : RecyclerView.Adapter<AvailableProductAdapter.AvailableProductViewHolder>() {

    private var products = emptyList<ProductsEntity>()

    inner class AvailableProductViewHolder(val binding: AvailableProductListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductsEntity) {
            binding.tvProductNameAvailable.text = product.name
            binding.tvProductPriceAvailable.text = String.format(Locale.ROOT, "Цена: %.2f", product.cost)
            binding.tvProductStockAvailable.text = "На складе: ${product.quantity} шт."

            binding.root.setOnClickListener {
                onProductClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableProductViewHolder {
        val binding = AvailableProductListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AvailableProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvailableProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun submitList(list: List<ProductsEntity>) {
        products = list
        notifyDataSetChanged()
    }
}