package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Customers")
data class CustomersEntity(
    @PrimaryKey(autoGenerate = true)
    var idCustomer: Int = 0, // Добавил = 0

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "address")
    var address: String?, 


    @ColumnInfo(name = "phone")
    var phone: String,

    @ColumnInfo(name = "contactPersonEmail")
    var contactPersonEmail: String
)