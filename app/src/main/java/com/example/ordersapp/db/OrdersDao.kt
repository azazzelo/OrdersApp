package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OrdersDao {

    @Insert
    fun insert(order: OrdersEntity)

    @Query("DELETE FROM Orders WHERE idOrder = :idOrder")
    fun delete(idOrder: Int)

    @Query("SELECT * FROM Orders WHERE idCustomer = :idCustomer")
    fun select(idCustomer: Int)
}