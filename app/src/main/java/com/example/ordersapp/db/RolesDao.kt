package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy // Используется для insert
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for Roles (Roles table).
 */
@Dao
interface RolesDao {

    /**
     * Inserts a new role. If a role with the same ID already exists, it will be ignored.
     * Use this for inserting known roles initially or new ones.
     * @return The row ID of the newly inserted role, or -1 if ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(role: RolesEntity): Long // Метод insert ЕСТЬ

    /**
     * Updates an existing role.
     * @return The number of rows updated (should be 1).
     */
    @Update
    suspend fun update(role: RolesEntity): Int // Метод update ЕСТЬ

    /**
     * Deletes a role.
     * @return The number of rows deleted (should be 1).
     */
    @Delete
    suspend fun delete(role: RolesEntity): Int // Метод delete ЕСТЬ

    /**
     * Gets a role by its ID.
     */
    @Query("SELECT * FROM Roles WHERE idRole = :roleId")
    suspend fun getRoleById(roleId: Int): RolesEntity? // Метод getRoleById ЕСТЬ

    /**
     * Gets all roles, sorted by name.
     */
    @Query("SELECT * FROM Roles ORDER BY name ASC")
    suspend fun getAllRoles(): List<RolesEntity> // Метод getAllRoles ЕСТЬ
}