package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProductsDao {

    @Insert
    fun insert(product: ProductsEntity)

    @Query("DELETE FROM Products WHERE idProduct = :idProduct")
    fun delete(idProduct: Int)

    @Query("SELECT * FROM Products WHERE name = :name")
    fun select(name: String)
}