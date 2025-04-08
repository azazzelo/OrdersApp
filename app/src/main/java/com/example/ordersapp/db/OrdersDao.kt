package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object для работы с заказами (таблица Orders).
 */
@Dao
interface OrdersDao {

    /**
     * Вставка нового заказа.
     * Возвращает ID вставленного заказа.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) // Стандартное поведение
    suspend fun insertOrder(order: OrdersEntity): Long

    /**
     * Обновление существующего заказа.
     * Возвращает количество обновленных строк.
     */
    @Update
    suspend fun updateOrder(order: OrdersEntity): Int

    /**
     * Удаление заказа.
     * Возвращает количество удаленных строк.
     */
    @Delete
    suspend fun deleteOrder(order: OrdersEntity): Int

    /**
     * Получение всех заказов, отсортированных по дате регистрации (сначала новые).
     */
    @Query("SELECT * FROM Orders ORDER BY dateRegistration DESC")
    suspend fun getAllOrders(): List<OrdersEntity>

    /**
     * Получение заказа по его ID.
     */
    @Query("SELECT * FROM Orders WHERE idOrder = :orderId")
    suspend fun getOrderById(orderId: Int): OrdersEntity?

    /**
     * Получение всех заказов для конкретного заказчика.
     */
    @Query("SELECT * FROM Orders WHERE idCustomer = :customerId ORDER BY dateRegistration DESC")
    suspend fun getOrdersByCustomerId(customerId: Int): List<OrdersEntity>

    /**
     * Получение общей суммы заказов за указанный год и месяц.
     * Даты хранятся как Long (timestamp в мс), поэтому используем strftime с unixepoch.
     * :yearMonth должен быть в формате "YYYY-MM" (например, "2024-07").
     */
    @Query("SELECT SUM(finallyCost) FROM Orders WHERE strftime('%Y-%m', dateRegistration / 1000, 'unixepoch') = :yearMonth")
    suspend fun getMonthlyRevenue(yearMonth: String): Double? // Возвращает Double или null, если нет заказов


    /**
     * Получение списка самых продаваемых товаров за указанный период (по убыванию количества).
     * Возвращает список ProductSaleInfo (ID, Название, Общее проданное количество).
     * Даты передаются как Long timestamp (миллисекунды).
     * Включаем только заказы в указанном диапазоне дат.
     *
     * @param startDateTimestamp Начало периода (включительно).
     * @param endDateTimestamp Конец периода (включительно).
     * @param limit Ограничение на количество возвращаемых топ-товаров.
     * @return Список ProductSaleInfo.
     */
    @Query("""
        SELECT
            p.idProduct,
            p.name AS name, 
            SUM(op.quantity) AS totalQuantitySold
        FROM Products p
        JOIN `Orders-Products` op ON p.idProduct = op.idProduct
        JOIN Orders o ON op.idOrder = o.idOrder
        WHERE o.dateRegistration BETWEEN :startDateTimestamp AND :endDateTimestamp
        GROUP BY p.idProduct, p.name
        ORDER BY totalQuantitySold DESC
        LIMIT :limit
    """)
    suspend fun getTopSellingProducts(startDateTimestamp: Long, endDateTimestamp: Long, limit: Int = 5): List<ProductSaleInfo>

}