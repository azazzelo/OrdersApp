package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RolesDao {
    @Insert
    fun insert(role: RolesEntity)

    @Query("DELETE FROM Roles WHERE idRole = :idRole")
    fun delete(idRole: Int)


}