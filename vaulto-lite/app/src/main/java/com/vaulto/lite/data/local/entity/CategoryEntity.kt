package com.vaulto.lite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

/** Seeded on first launch. */
val DefaultCategories = listOf(
    CategoryEntity(name = "Food", emoji = "🍔", colorHex = "#FF9F43", isDefault = true),
    CategoryEntity(name = "Transport", emoji = "🚌", colorHex = "#E2725B", isDefault = true),
    CategoryEntity(name = "Shopping", emoji = "🛍️", colorHex = "#E3B23C", isDefault = true),
    CategoryEntity(name = "Bills", emoji = "🧾", colorHex = "#9CAF88", isDefault = true),
    CategoryEntity(name = "Entertainment", emoji = "🎬", colorHex = "#7FA6C9", isDefault = true),
    CategoryEntity(name = "Other", emoji = "🗂️", colorHex = "#A682A8", isDefault = true)
)
