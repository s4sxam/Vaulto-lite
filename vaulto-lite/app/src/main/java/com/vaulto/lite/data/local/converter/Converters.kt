package com.vaulto.lite.data.local.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vaulto.lite.data.local.entity.RecurrenceType

@ProvidedTypeConverter
class MapConverter {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<Long, Double>>() {}.type

    @TypeConverter
    fun fromMap(map: Map<Long, Double>?): String =
        gson.toJson(map ?: emptyMap<Long, Double>())

    @TypeConverter
    fun toMap(json: String?): Map<Long, Double> =
        if (json.isNullOrBlank()) emptyMap()
        else gson.fromJson(json, mapType) ?: emptyMap()
}

class RecurrenceTypeConverter {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType =
        runCatching { RecurrenceType.valueOf(value) }.getOrDefault(RecurrenceType.NONE)
}
