package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index // Импортируем Index

@Entity(
    tableName = "Orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomersEntity::class,
            parentColumns = ["idCustomer"],
            childColumns = ["idCustomer"],
            // onDelete = ForeignKey.SET_NULL // Или CASCADE, если заказы должны удаляться при удалении клиента
            // onUpdate = ForeignKey.CASCADE
        )
    ],
    // Добавляем индекс для ускорения поиска по idCustomer
    indices = [Index(value = ["idCustomer"])]
)
data class OrdersEntity(
    @PrimaryKey(autoGenerate = true)
    val idOrder: Int = 0, // Устанавливаем значение по умолчанию

    @ColumnInfo(name = "idCustomer")
    var idCustomer: Int,

    @ColumnInfo(name = "dateRegistration")
    var dateRegistration: Long, // Изменено на Long (timestamp)

    @ColumnInfo(name = "dateDelivery")
    var dateDelivery: Long, // Изменено на Long (timestamp)

    @ColumnInfo(name = "finallyCost")
    var finallyCost: Double // Изменено на Double для точности цен
)