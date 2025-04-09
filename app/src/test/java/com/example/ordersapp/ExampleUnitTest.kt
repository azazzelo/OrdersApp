package com.example.ordersapp

import com.example.ordersapp.adapters.ProductInOrderDisplay
import com.example.ordersapp.db.ProductsEntity
import com.example.ordersapp.utils.formatCurrency


import org.junit.Test
import org.junit.Assert.*

class OrderCalculationsTest {



    @Test
    fun calculateTotalCost_singleItem_returnsCorrectCost() {

        val product = ProductsEntity(1, "Item 1", 10.50, 5, null)
        val productsInOrder = listOf(ProductInOrderDisplay(product, 3)) // 3 * 10.50

        val result = calculateTotalCostForTesting(productsInOrder)

        assertEquals("Cost of 3 * 10.50 should be 31.50", 31.50, result, 0.001)
    }

    @Test
    fun formatCurrency_zeroValue_isCorrect() {
        assertEquals("$0.00", formatCurrency(0.0))
    }
}