package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomersEntity::class,
            parentColumns = ["idCustomer"],
            childColumns = ["idCustomer"]
        )
    ]
)
data class OrdersEntity(
    @PrimaryKey(autoGenerate = true)
    val idOrder: Int,
    @ColumnInfo(name = "idCustomer")
    var idCustomer: Int,
    @ColumnInfo(name = "dateRegistration")
    var dateRegistration: String,
    @ColumnInfo(name = "dateDelivery")
    var dateDelivery: Int,
    @ColumnInfo(name = "finallyCost")
    var finallyCost: Int,


    )
