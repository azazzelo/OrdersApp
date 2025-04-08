package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface OrdersProductsDao {

    /**
     * Вставка связи "заказ-продукт". Если такая связь уже есть, она будет заменена (полезно при обновлении).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderProduct(orderProduct: OrdersProductsEntity)

    /**
     * Вставка списка связей "заказ-продукт". Используется для сохранения всех товаров заказа разом.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderProducts(orderProducts: List<OrdersProductsEntity>)

    /**
     * Обновление количества продукта в конкретном заказе.
     * Можно использовать insert с OnConflictStrategy.REPLACE вместо отдельного update.
     */
    // @Update
    // suspend fun updateOrderProduct(orderProduct: OrdersProductsEntity): Int

    /**
     * Удаление конкретного продукта из заказа.
     */
    @Query("DELETE FROM `Orders-Products` WHERE idOrder = :orderId AND idProduct = :productId")
    suspend fun deleteOrderProduct(orderId: Int, productId: Int): Int

    /**
     * Удаление ВСЕХ продуктов для указанного заказа.
     * Используется перед обновлением списка товаров в заказе или при удалении самого заказа.
     */
    @Query("DELETE FROM `Orders-Products` WHERE idOrder = :orderId")
    suspend fun deleteProductsForOrder(orderId: Int): Int

    /**
     * Получение списка всех продуктов (их ID и количество) для указанного заказа.
     */
    @Query("SELECT * FROM `Orders-Products` WHERE idOrder = :orderId")
    suspend fun getProductsForOrder(orderId: Int): List<OrdersProductsEntity>

    // Метод для получения деталей товаров (имя, цена) будет сложнее,
    // его можно добавить позже или получать детали в ViewModel/Activity,
    // загружая продукты по ID из ProductsDao.

}