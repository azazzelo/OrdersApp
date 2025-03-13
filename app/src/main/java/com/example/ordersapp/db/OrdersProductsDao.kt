package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OrdersProductsDao {

    @Insert
    fun insert(orderProduct: OrdersProductsEntity)

    @Query("DELETE FROM `Orders-Products` WHERE idProduct = :idProduct")
    fun delete(idProduct: Int)

}