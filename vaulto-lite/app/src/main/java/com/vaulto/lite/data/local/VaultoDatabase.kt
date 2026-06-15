package com.vaulto.lite.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vaulto.lite.data.local.converter.MapConverter
import com.vaulto.lite.data.local.converter.RecurrenceTypeConverter
import com.vaulto.lite.data.local.dao.BudgetDao
import com.vaulto.lite.data.local.dao.CategoryDao
import com.vaulto.lite.data.local.dao.ExpenseDao
import com.vaulto.lite.data.local.entity.BudgetEntity
import com.vaulto.lite.data.local.entity.CategoryEntity
import com.vaulto.lite.data.local.entity.ExpenseEntity

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(MapConverter::class, RecurrenceTypeConverter::class)
abstract class VaultoDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        private const val DB_NAME = "vaulto_lite.db"

        @Volatile
        private var INSTANCE: VaultoDatabase? = null

        fun getInstance(context: Context): VaultoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultoDatabase::class.java,
                    DB_NAME
                )
                    .addTypeConverter(MapConverter())
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
