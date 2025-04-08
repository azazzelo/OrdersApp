package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Products")
data class ProductsEntity(
    @PrimaryKey(autoGenerate = true)
    var idProduct: Int,
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "cost")
    var cost: Double,
    @ColumnInfo(name = "quantity")
    var quantity: Int,
    @ColumnInfo(name = "photo_uri")
    var photoUri: String? = null
)
