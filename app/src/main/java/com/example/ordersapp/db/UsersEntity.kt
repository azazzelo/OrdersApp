package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Users",
    foreignKeys = [
        ForeignKey(entity = RolesEntity::class, parentColumns = ["idRole"], childColumns = ["idRole"])
    ]
)
data class UsersEntity(
    @PrimaryKey(autoGenerate = true)
    var idUser: Int,
    @ColumnInfo(name = "idRole")
    var idRole: Int,
    @ColumnInfo(name = "login")
    var login: String,
    @ColumnInfo(name = "password")
    var password: String
)
