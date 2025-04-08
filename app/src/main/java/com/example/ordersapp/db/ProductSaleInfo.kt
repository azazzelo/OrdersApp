package com.example.ordersapp.db

import androidx.room.ColumnInfo

/**
 * Data class для хранения результата запроса о продажах товара.
 * Используется в OrdersDao.getTopSellingProducts.
 */
data class ProductSaleInfo(
    // Указываем имя колонки из запроса, чтобы Room знал, куда мапить
    @ColumnInfo(name = "idProduct")
    val productId: Int,

    // Имя из таблицы Products
    @ColumnInfo(name = "name")
    val productName: String,

    // Агрегированное значение (сумма) из запроса
    @ColumnInfo(name = "totalQuantitySold")
    val totalQuantitySold: Int
)