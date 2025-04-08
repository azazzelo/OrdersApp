package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction // Для транзакций при обновлении количества

@Dao
interface ProductsDao {
    @Insert
    suspend fun insert(product: ProductsEntity): Long

    @Update
    suspend fun update(product: ProductsEntity): Int

    @Delete
    suspend fun delete(product: ProductsEntity): Int

    @Query("SELECT * FROM Products ORDER BY name ASC")
    suspend fun getAllProducts(): List<ProductsEntity>

    @Query("SELECT * FROM Products WHERE idProduct = :id")
    suspend fun getProductById(id: Int): ProductsEntity?

    // --- Добавленные методы ---

    /**
     * Поиск товаров по имени (частичное совпадение).
     */
    @Query("SELECT * FROM Products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchProductsByName(query: String): List<ProductsEntity>

    /**
     * Получение списка товаров, которые есть в наличии (количество > 0).
     */
    @Query("SELECT * FROM Products WHERE quantity > 0 ORDER BY name ASC")
    suspend fun getAvailableProducts(): List<ProductsEntity>

    /**
     * Уменьшение количества товара на складе.
     * ВАЖНО: Вызывать внутри транзакции вместе с созданием/обновлением заказа.
     * Возвращает количество обновленных строк (должно быть 1 при успехе).
     * Добавлена проверка на достаточное количество перед обновлением.
     */
    @Query("UPDATE Products SET quantity = quantity - :amount WHERE idProduct = :productId AND quantity >= :amount")
    suspend fun decreaseProductQuantity(productId: Int, amount: Int): Int

    /**
     * Увеличение количества товара на складе (например, при отмене заказа).
     * Возвращает количество обновленных строк.
     */
    @Query("UPDATE Products SET quantity = quantity + :amount WHERE idProduct = :productId")
    suspend fun increaseProductQuantity(productId: Int, amount: Int): Int

}