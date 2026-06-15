package com.vaulto.lite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One Budget row per month/year. [categoryBudgets] maps categoryId -> allocated amount
 * and is persisted as JSON via [com.vaulto.lite.data.local.converter.MapConverter].
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val amount: Double, // overall monthly budget
    val categoryBudgets: Map<Long, Double> = emptyMap()
)
