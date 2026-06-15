package com.vaulto.lite.data.settings

/** User-selectable appearance mode. SYSTEM follows the device dark-mode setting. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        val DEFAULT = SYSTEM

        fun fromNameOrDefault(name: String?): ThemeMode =
            entries.find { it.name == name } ?: DEFAULT
    }
}
