package com.example.ordersapp.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UsersDao {
    @Insert
    suspend fun insert(user: UsersEntity)

    @Query("SELECT * FROM Users WHERE login = :login AND password = :password")
    suspend fun getUserByCredentials(login: String, password: String): UsersEntity?

    @Query("SELECT * FROM Users WHERE idRole = :roleId")
    suspend fun getUsersByRole(roleId: Int): List<UsersEntity>

    @Query("SELECT * FROM Users WHERE idUser = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UsersEntity?

    @Query("SELECT * FROM Users")
    suspend fun getAllUsers(): List<UsersEntity>

    @Update
    suspend fun updateUser(user: UsersEntity)

    @Delete
    suspend fun deleteUser(user: UsersEntity)

}