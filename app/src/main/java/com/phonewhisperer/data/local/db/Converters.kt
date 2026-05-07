package com.phonewhisperer.data.local.db

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converters for non-primitive types.
 *
 * Room can only persist primitives and strings natively. These converters
 * handle Date <-> Long conversions. Additional converters for List<String>,
 * custom enums, etc. can be added here as the schema evolves.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
