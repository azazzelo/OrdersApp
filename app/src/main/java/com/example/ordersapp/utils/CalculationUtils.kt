package com.example.ordersapp.utils


import com.example.ordersapp.adapters.ProductInOrderDisplay
import java.util.Locale


fun calculateTotalOrderCost(products: List<ProductInOrderDisplay>): Double {

    if (products.isEmpty()) {
        return 0.0
    }
    return products.sumOf { (it.product.cost ?: 0.0) * it.quantityInOrder }
}


fun formatCurrency(value: Double?, currencySymbol: String = "$"): String {
    return String.format(Locale.US, "%s%.2f", currencySymbol, value ?: 0.0)
}