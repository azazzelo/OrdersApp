package com.example.ordersapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Roles")
data class RolesEntity(
    @PrimaryKey(autoGenerate = true)
    var idRole: Int,
    @ColumnInfo(name = "name")
    var name: String
)
