package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Orders-Products",
    primaryKeys = ["idOrder", "idProduct"],
    foreignKeys = [
        ForeignKey(
            entity = OrdersEntity::class,
            parentColumns = ["idOrder"],
            childColumns = ["idOrder"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductsEntity::class,
            parentColumns = ["idProduct"],
            childColumns = ["idProduct"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class OrdersProductsEntity(
    @ColumnInfo(name = "idOrder") val idOrder: Int,
    @ColumnInfo(name = "idProduct") val idProduct: Int,
    @ColumnInfo(name = "quantity") val quantity: Int
)
