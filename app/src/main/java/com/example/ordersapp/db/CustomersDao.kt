package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CustomersDao {

    @Insert
    fun insert(customer: CustomersEntity)

    @Query("DELETE FROM Customers WHERE idCustomer = :idCustomer")
    fun delete(idCustomer: Int)

    @Query("SELECT * FROM Customers WHERE name = :name")
    fun select(name: String)
}