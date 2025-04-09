package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
// Импортируем аннотацию для запросов с переменными аргументами (хотя она здесь не нужна)
// import androidx.room.RawQuery
// import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface OrdersDao {

    // --- Существующие методы ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: OrdersEntity): Long

    @Update
    suspend fun updateOrder(order: OrdersEntity): Int

    @Delete
    suspend fun deleteOrder(order: OrdersEntity): Int

    // Убираем старый getAllOrders, так как новый метод его заменит
    // @Query("SELECT * FROM Orders ORDER BY dateRegistration DESC")
    // suspend fun getAllOrders(): List<OrdersEntity>

    @Query("SELECT * FROM Orders WHERE idOrder = :orderId")
    suspend fun getOrderById(orderId: Int): OrdersEntity?

    @Query("SELECT * FROM Orders WHERE idCustomer = :customerId ORDER BY dateRegistration DESC")
    suspend fun getOrdersByCustomerId(customerId: Int): List<OrdersEntity>

    @Query("SELECT SUM(finallyCost) FROM Orders WHERE strftime('%Y-%m', dateRegistration / 1000, 'unixepoch') = :yearMonth")
    suspend fun getMonthlyRevenue(yearMonth: String): Double?

    @Query("""
        SELECT p.idProduct, p.name AS name, SUM(op.quantity) AS totalQuantitySold
        FROM Products p JOIN `Orders-Products` op ON p.idProduct = op.idProduct JOIN Orders o ON op.idOrder = o.idOrder
        WHERE o.dateRegistration BETWEEN :startDateTimestamp AND :endDateTimestamp
        GROUP BY p.idProduct, p.name ORDER BY totalQuantitySold DESC LIMIT :limit
    """)
    suspend fun getTopSellingProducts(startDateTimestamp: Long, endDateTimestamp: Long, limit: Int = 5): List<ProductSaleInfo>


    // --- НОВЫЙ МЕТОД для поиска, фильтрации и сортировки ---
    /**
     * Получает список заказов с возможностью поиска, фильтрации по дате и сортировки.
     *
     * @param searchQuery Строка поиска. Ищет по ID заказа или ID клиента. Если пустая, поиск не применяется.
     * @param startDate Начальная дата периода (timestamp, Long). Если null, не фильтрует по начальной дате.
     * @param endDate Конечная дата периода (timestamp, Long). Если null, не фильтрует по конечной дате.
     * @param sortBy Поле для сортировки ("dateRegistration", "dateDelivery", "finallyCost"). По умолчанию "dateRegistration".
     * @param sortOrder Порядок сортировки ("ASC" - по возрастанию, "DESC" - по убыванию). По умолчанию "DESC".
     * @return Список отфильтрованных и отсортированных заказов.
     */
    @Query("""
        SELECT * FROM Orders
        WHERE
            (:searchQuery = '' OR CAST(idOrder AS TEXT) LIKE '%' || :searchQuery || '%' OR CAST(idCustomer AS TEXT) LIKE '%' || :searchQuery || '%')
            AND (:startDate IS NULL OR dateRegistration >= :startDate)
            AND (:endDate IS NULL OR dateRegistration <= :endDate)
        ORDER BY
            CASE WHEN :sortBy = 'dateRegistration' AND :sortOrder = 'ASC' THEN dateRegistration END ASC,
            CASE WHEN :sortBy = 'dateRegistration' AND :sortOrder = 'DESC' THEN dateRegistration END DESC,
            CASE WHEN :sortBy = 'dateDelivery' AND :sortOrder = 'ASC' THEN dateDelivery END ASC,
            CASE WHEN :sortBy = 'dateDelivery' AND :sortOrder = 'DESC' THEN dateDelivery END DESC,
            CASE WHEN :sortBy = 'finallyCost' AND :sortOrder = 'ASC' THEN finallyCost END ASC,
            CASE WHEN :sortBy = 'finallyCost' AND :sortOrder = 'DESC' THEN finallyCost END DESC,
            dateRegistration DESC /* Сортировка по умолчанию, если sortBy некорректный */
    """)
    suspend fun getOrdersFilteredSorted(
        searchQuery: String = "", // Поиск по ID заказа или клиента
        startDate: Long? = null, // Фильтр по дате регистрации >= startDate
        endDate: Long? = null,   // Фильтр по дате регистрации <= endDate
        sortBy: String = "dateRegistration", // Поле сортировки
        sortOrder: String = "DESC" // Порядок сортировки (ASC/DESC)
    ): List<OrdersEntity>

    // Примечание: Использование CASE в ORDER BY может быть не самым быстрым способом
    // для сложных сортировок в SQLite, но оно гибкое. Альтернатива - RawQuery,
    // но он сложнее в использовании с параметрами и проверкой типов.

}