package com.phonewhisperer.execution

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.receiver.RuleExecutionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact alarms for time-based automation rules.
 *
 * Uses Android's AlarmManager to fire at the exact trigger time defined
 * in each approved rule (e.g., "09:00"). The alarm triggers
 * [RuleExecutionReceiver], which fetches the rule and invokes [ActionExecutor].
 *
 * Alarms are re-created:
 *   - When the user approves a new rule
 *   - On device reboot (via BootReceiver)
 *   - After each alarm fires (next-day reschedule)
 */
@Singleton
class TimeRuleScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "TimeRuleScheduler"
        const val EXTRA_RULE_ID = "extra_rule_id"
        private const val REQUEST_CODE_BASE = 10000 // Offset to avoid collision
    }

    /**
     * Schedules an exact alarm for a time-based rule.
     *
     * @param rule The approved automation rule with triggerType == "TIME"
     */
    fun scheduleRule(rule: AutomationRuleEntity) {
        if (rule.triggerType != "TIME") {
            Log.d(TAG, "Skipping non-time rule: ${rule.name}")
            return
        }

        val triggerTimeMs = parseTimeToNextOccurrence(rule.triggerValue)
        if (triggerTimeMs <= 0) {
            Log.e(TAG, "Failed to parse trigger time: ${rule.triggerValue}")
            return
        }

        val pendingIntent = createPendingIntent(rule.id)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact permission not granted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarm permission not granted — using inexact alarm")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }

            val calendar = Calendar.getInstance().apply { timeInMillis = triggerTimeMs }
            Log.d(TAG, "✓ Alarm scheduled: '${rule.name}' → ${calendar.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", calendar.get(Calendar.MINUTE))}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm — missing SCHEDULE_EXACT_ALARM", e)
        }
    }

    /**
     * Cancels a scheduled alarm for a specific rule.
     */
    fun cancelRule(ruleId: Long) {
        val pendingIntent = createPendingIntent(ruleId)
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled for rule ID: $ruleId")
    }

    /**
     * Reschedules a rule for the next day (called after an alarm fires).
     */
    fun rescheduleForNextDay(rule: AutomationRuleEntity) {
        scheduleRule(rule)
    }

    /**
     * Schedules alarms for all provided rules (used on reboot).
     */
    fun scheduleAllRules(rules: List<AutomationRuleEntity>) {
        for (rule in rules) {
            scheduleRule(rule)
        }
        Log.d(TAG, "Scheduled ${rules.size} alarms after reboot")
    }

    /**
     * Parses a time string like "9:00" or "21:30" into the next calendar
     * occurrence in milliseconds. If the time has already passed today,
     * it schedules for tomorrow.
     */
    private fun parseTimeToNextOccurrence(timeStr: String): Long {
        return try {
            val parts = timeStr.replace(" ", "").split(":")
            val hour = parts[0].toInt()
            val minute = if (parts.size > 1) parts[1].toInt() else 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time: $timeStr", e)
            -1L
        }
    }

    private fun createPendingIntent(ruleId: Long): PendingIntent {
        val intent = Intent(context, RuleExecutionReceiver::class.java).apply {
            putExtra(EXTRA_RULE_ID, ruleId)
            action = "com.phonewhisperer.ACTION_EXECUTE_RULE_$ruleId"
        }
        return PendingIntent.getBroadcast(
            context,
            (REQUEST_CODE_BASE + ruleId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
