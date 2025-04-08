package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy // Для insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CustomersDao {

    /**
     * Вставка нового заказчика. Если заказчик с таким ID уже существует, ничего не делать.
     * Возвращает ID вставленной строки.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customer: CustomersEntity): Long

    /**
     * Обновление существующего заказчика.
     * Возвращает количество обновленных строк (должно быть 1).
     */
    @Update
    suspend fun update(customer: CustomersEntity): Int

    /**
     * Удаление заказчика.
     * Возвращает количество удаленных строк (должно быть 1).
     */
    @Delete
    suspend fun delete(customer: CustomersEntity): Int

    /**
     * Получение всех заказчиков, отсортированных по имени.
     */
    @Query("SELECT * FROM Customers ORDER BY name ASC")
    suspend fun getAllCustomers(): List<CustomersEntity>

    /**
     * Получение заказчика по его ID.
     */
    @Query("SELECT * FROM Customers WHERE idCustomer = :id")
    suspend fun getCustomerById(id: Int): CustomersEntity?

    /**
     * Поиск заказчиков по имени или email (частичное совпадение, без учета регистра).
     */
    @Query("SELECT * FROM Customers WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(contactPersonEmail) LIKE '%' || LOWER(:query) || '%' ORDER BY name ASC")
    suspend fun searchCustomers(query: String): List<CustomersEntity>

}