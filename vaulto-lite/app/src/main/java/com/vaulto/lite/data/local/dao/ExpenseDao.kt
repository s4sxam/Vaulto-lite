package com.vaulto.lite.data.local.dao

import androidx.room.*
import com.vaulto.lite.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE month = :month AND year = :year ORDER BY date DESC, id DESC")
    fun observeForMonth(month: Int, year: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startMillis AND :endMillis ORDER BY date ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date ASC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE month = :month AND year = :year")
    fun observeTotalForMonth(month: Int, year: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE month = :month AND year = :year AND categoryId = :categoryId")
    fun observeCategoryTotalForMonth(categoryId: Long, month: Int, year: Int): Flow<Double>

    @Query("SELECT * FROM expenses WHERE isRecurring = 1")
    fun observeRecurring(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
