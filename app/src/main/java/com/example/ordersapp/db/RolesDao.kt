package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RolesDao {

    @Insert
    suspend fun insert(role: RolesEntity)

    @Query("SELECT * FROM Roles WHERE idRole = :roleId")
    suspend fun getRoleById(roleId: Int): RolesEntity?

    @Query("SELECT * FROM Roles")
    suspend fun getAllRoles(): List<RolesEntity>
}