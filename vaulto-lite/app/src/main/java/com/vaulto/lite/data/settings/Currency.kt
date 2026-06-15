package com.vaulto.lite.data.settings

/**
 * Supported currencies for display formatting. Persisted by name (string)
 * in DataStore via [SettingsRepository].
 */
enum class Currency(val code: String, val symbol: String, val displayName: String) {
    INR("INR", "₹", "Indian Rupee"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    JPY("JPY", "¥", "Japanese Yen");

    companion object {
        val DEFAULT = INR

        fun fromCodeOrDefault(code: String?): Currency =
            entries.find { it.code == code } ?: DEFAULT
    }
}
