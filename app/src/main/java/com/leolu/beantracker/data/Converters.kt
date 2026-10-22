package com.leolu.beantracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromList(values: List<String>): String = values.joinToString(SEPARATOR)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        // ASCII unit separator: never typed by users, so values may contain "/" or ",".
        const val SEPARATOR = "\u001F"
    }
}
