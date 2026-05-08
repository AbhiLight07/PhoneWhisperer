package com.phonewhisperer.data.collector

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.phonewhisperer.data.local.db.dao.BehaviorEventDao
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

/**
 * Collects information about the next scheduled alarm to build routines
 * based on wake-up times and alarm schedules.
 */
class AlarmCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val behaviorEventDao: BehaviorEventDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("phonewhisperer_prefs", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun collectNextAlarm(): Int {
        val nextAlarmInfo = alarmManager.nextAlarmClock
        if (nextAlarmInfo == null) {
            Log.d("AlarmCollector", "No upcoming alarms scheduled.")
            return 0
        }

        val triggerTime = nextAlarmInfo.triggerTime
        val lastLoggedTrigger = prefs.getLong("last_logged_alarm_trigger", 0L)

        // Deduplicate: Don't log if we already logged this exact alarm trigger time
        if (triggerTime == lastLoggedTrigger) {
            Log.d("AlarmCollector", "Alarm at $triggerTime already logged. Skipping.")
            return 0
        }

        val packageName = nextAlarmInfo.showIntent?.creatorPackage ?: "unknown"
        
        val payload = JSONObject().apply {
            put("triggerTime", triggerTime)
            put("packageName", packageName)
        }.toString()

        val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val event = BehaviorEvent(
            eventType = "TYPE_ALARM",
            timestamp = System.currentTimeMillis(),
            dayOfWeek = dayOfWeek,
            hourOfDay = hourOfDay,
            payload = payload
        )

        behaviorEventDao.insert(event)
        prefs.edit().putLong("last_logged_alarm_trigger", triggerTime).apply()

        Log.d("AlarmCollector", "Logged new alarm for $triggerTime ($packageName)")
        return 1
    }
}
