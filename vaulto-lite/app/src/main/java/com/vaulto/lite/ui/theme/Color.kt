package com.vaulto.lite.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme palette - warm cream + saffron
val CreamBackground = Color(0xFFFFF8F0)
val CreamSurface = Color(0xFFFFFFFF)
val CreamSurfaceVariant = Color(0xFFFCEFE3)
val SaffronPrimary = Color(0xFFFF9F43)
val SaffronPrimaryDark = Color(0xFFE8893A)
val OnSaffron = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF2D2A26)
val TextSecondaryLight = Color(0xFF8C8478)

// Dark theme palette - deep charcoal + same saffron accent
val CharcoalBackground = Color(0xFF1C1B1A)
val CharcoalSurface = Color(0xFF262422)
val CharcoalSurfaceVariant = Color(0xFF332F2B)
val TextPrimaryDark = Color(0xFFF5F0EA)
val TextSecondaryDark = Color(0xFFA89E93)

// Semantic status colors (progress bar: green -> amber -> red)
val StatusGreen = Color(0xFF4CAF50)
val StatusAmber = Color(0xFFFFB347)
val StatusRed = Color(0xFFE85D5D)

// Delta colors for analytics (red = spending more, green = less)
val DeltaPositiveBad = Color(0xFFE85D5D) // spending increased
val DeltaNegativeGood = Color(0xFF4CAF50) // spending decreased

// Category / chart palette - warm complementary set
val ChartSaffron = Color(0xFFFF9F43)
val ChartTerracotta = Color(0xFFE2725B)
val ChartMustard = Color(0xFFE3B23C)
val ChartSage = Color(0xFF9CAF88)
val ChartDustyBlue = Color(0xFF7FA6C9)
val ChartPlum = Color(0xFFA682A8)
val ChartTeal = Color(0xFF5FAFA0)

/**
 * Default ordered palette assigned to categories. Persisted mapping is stored
 * per-category in [com.vaulto.lite.data.local.entity.CategoryEntity.colorHex].
 */
val DefaultCategoryPalette = listOf(
    ChartSaffron,
    ChartTerracotta,
    ChartMustard,
    ChartSage,
    ChartDustyBlue,
    ChartPlum,
    ChartTeal
)
