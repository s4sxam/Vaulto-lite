package com.vaulto.lite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val amount: Double,
    val note: String? = null,
    val month: Int,   // 1-12
    val year: Int,    // e.g. 2026
    val date: Long,   // epoch millis (start of day)
    val isRecurring: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE
)
