package com.example.ordersapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsersDao {

    @Insert
    fun insert(user: UsersEntity)

    @Query("DELETE FROM Users WHERE idUser = :idUser")
    fun deleteUser(idUser: Int)

    @Query("SELECT * FROM Users WHERE login = :login")
    fun select(login: String)
}