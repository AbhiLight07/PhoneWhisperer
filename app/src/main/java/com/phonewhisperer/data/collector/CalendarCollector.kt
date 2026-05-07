package com.phonewhisperer.data.collector

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.phonewhisperer.data.local.db.dao.BehaviorEventDao
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects upcoming calendar events from the device's calendar.
 *
 * Design decisions:
 * - Reads events for the next 24 hours (forward-looking, not historical).
 * - Creates BehaviorEvent entries with TYPE_CALENDAR so the LLM can correlate
 *   calendar patterns with other behaviors (e.g., "DND during meetings").
 * - Payload includes event title, location, and start/end times as JSON.
 * - Only reads — never writes to the user's calendar.
 */
@Singleton
class CalendarCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val behaviorEventDao: BehaviorEventDao
) {
    companion object {
        private const val TAG = "CalendarCollector"
        private const val LOOK_AHEAD_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Collects calendar events for the next 24 hours.
     *
     * @return Number of calendar events inserted.
     */
    suspend fun collectUpcomingEvents(): Int {
        if (!hasCalendarPermission()) {
            Log.w(TAG, "Calendar permission not granted, skipping")
            return 0
        }

        val now = System.currentTimeMillis()
        val endTime = now + LOOK_AHEAD_MS

        // Build the query URI with time range
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, endTime)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )

        var insertedCount = 0

        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
                val locationIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val calNameIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(idIndex)
                    val title = cursor.getString(titleIndex) ?: "Untitled"
                    val begin = cursor.getLong(beginIndex)
                    val end = cursor.getLong(endIndex)
                    val location = cursor.getString(locationIndex) ?: ""
                    val isAllDay = cursor.getInt(allDayIndex) == 1
                    val calendarName = cursor.getString(calNameIndex) ?: ""

                    val payload = JSONObject().apply {
                        put("eventId", eventId)
                        put("title", title)
                        put("startTime", begin)
                        put("endTime", end)
                        put("location", location)
                        put("isAllDay", isAllDay)
                        put("calendarName", calendarName)
                        put("durationMinutes", (end - begin) / 60_000)
                    }

                    val calendar = Calendar.getInstance().apply { timeInMillis = begin }

                    val behaviorEvent = BehaviorEvent(
                        timestamp = begin,
                        eventType = BehaviorEvent.TYPE_CALENDAR,
                        payload = payload.toString(),
                        dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                    )

                    behaviorEventDao.insert(behaviorEvent)
                    insertedCount++
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception reading calendar", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading calendar events", e)
        }

        Log.d(TAG, "Collected $insertedCount upcoming calendar events")
        return insertedCount
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
}
