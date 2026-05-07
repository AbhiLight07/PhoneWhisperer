package com.phonewhisperer.utils

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.repository.EventRepository
import com.phonewhisperer.workers.PatternAnalysisWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Generates rich multi-pattern mock data for Phase 6 testing.
 *
 * Instead of a single pattern, simulates a realistic behavioral profile:
 *   1. Silent mode every weekday at 9 AM (college/work)
 *   2. Screen off around 11 PM (sleep pattern)
 *   3. Social media usage evenings 7-9 PM
 *   4. Notification dismissals during morning hours
 */
object MockDataGenerator {
    private const val TAG = "MockDataGenerator"

    fun injectMockDataAndRunAI(context: Context, eventRepository: EventRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "🔬 Injecting multi-pattern mock data...")

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            val events = mutableListOf<BehaviorEvent>()

            for (dayOffset in 0..21) {
                calendar.timeInMillis = now - (dayOffset * 24 * 60 * 60 * 1000L)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()

                // ── Pattern 1: Weekday Silent Mode at 9 AM ──────────────
                if (dayOfWeek in 1..5) {
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, (Math.random() * 12).toInt())
                    events.add(
                        BehaviorEvent(
                            timestamp = calendar.timeInMillis,
                            eventType = BehaviorEvent.TYPE_SILENT_MODE,
                            payload = """{"ringerMode":"SILENT","previousMode":"NORMAL"}""",
                            dayOfWeek = dayOfWeek,
                            hourOfDay = 9,
                            isProcessed = false
                        )
                    )
                }

                // ── Pattern 2: Screen off around 11 PM (Sleep) ──────────
                calendar.set(Calendar.HOUR_OF_DAY, 22 + (Math.random() * 2).toInt())
                calendar.set(Calendar.MINUTE, (Math.random() * 59).toInt())
                val sleepHour = calendar.get(Calendar.HOUR_OF_DAY)
                events.add(
                    BehaviorEvent(
                        timestamp = calendar.timeInMillis,
                        eventType = BehaviorEvent.TYPE_SCREEN_OFF,
                        payload = """{"batteryLevel":${(30..85).random()}}""",
                        dayOfWeek = dayOfWeek,
                        hourOfDay = sleepHour,
                        isProcessed = false
                    )
                )

                // ── Pattern 3: Social Media in Evenings ─────────────────
                if (Math.random() > 0.3) { // 70% of days
                    calendar.set(Calendar.HOUR_OF_DAY, 19 + (Math.random() * 2).toInt())
                    calendar.set(Calendar.MINUTE, (Math.random() * 59).toInt())
                    val socialHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val socialApps = listOf("com.instagram.android", "com.twitter.android", "com.snapchat.android")
                    events.add(
                        BehaviorEvent(
                            timestamp = calendar.timeInMillis,
                            eventType = BehaviorEvent.TYPE_APP_USAGE,
                            payload = """{"packageName":"${socialApps.random()}","duration":${(5..30).random()}}""",
                            dayOfWeek = dayOfWeek,
                            hourOfDay = socialHour,
                            isProcessed = false
                        )
                    )
                }

                // ── Pattern 4: Notification dismissals during work ──────
                if (dayOfWeek in 1..5 && Math.random() > 0.25) {
                    calendar.set(Calendar.HOUR_OF_DAY, 10 + (Math.random() * 3).toInt())
                    calendar.set(Calendar.MINUTE, (Math.random() * 59).toInt())
                    val notifHour = calendar.get(Calendar.HOUR_OF_DAY)
                    events.add(
                        BehaviorEvent(
                            timestamp = calendar.timeInMillis,
                            eventType = BehaviorEvent.TYPE_NOTIFICATION,
                            payload = """{"packageName":"com.whatsapp","appName":"WhatsApp","action":"DISMISSED"}""",
                            dayOfWeek = dayOfWeek,
                            hourOfDay = notifHour,
                            isProcessed = false
                        )
                    )
                }
            }

            eventRepository.insertBehaviorEvents(events)
            Log.d(TAG, "✓ Injected ${events.size} multi-pattern events")

            // Trigger AI Worker
            val aiWork = OneTimeWorkRequestBuilder<PatternAnalysisWorker>()
                .addTag(PatternAnalysisWorker.WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "manual_pattern_analysis",
                ExistingWorkPolicy.REPLACE,
                aiWork
            )
            Log.d(TAG, "✓ PatternAnalysisWorker triggered")
        }
    }

    private fun Int.toIsoDayOfWeek(): Int = when (this) {
        Calendar.SUNDAY -> 7
        else -> this - 1
    }
}
