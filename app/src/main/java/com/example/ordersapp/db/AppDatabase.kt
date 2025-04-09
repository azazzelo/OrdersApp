package com.example.ordersapp.db

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
    CustomersEntity::class,
    OrdersEntity::class,
    OrdersProductsEntity::class,
    ProductsEntity::class,
    RolesEntity::class,
    UsersEntity::class
    ],
    version = 3)
abstract class AppDatabase(): RoomDatabase() {
    abstract fun customersDao(): CustomersDao
    abstract fun ordersDao(): OrdersDao
    abstract fun ordersProductsDao(): OrdersProductsDao
    abstract fun productsDao(): ProductsDao
    abstract fun rolesDao(): RolesDao
    abstract fun usersDao(): UsersDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
