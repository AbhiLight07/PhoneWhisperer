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
 * Utility to generate mock data for Phase 3 testing.
 *
 * In a real environment, the AI engine requires 2-3 weeks of background data
 * to detect meaningful routines. For hackathon demonstration purposes, this
 * generator injects exactly 1 month of structured synthetic data.
 */
object MockDataGenerator {
    private const val TAG = "MockDataGenerator"

    /**
     * Seeds the database with a specific pattern:
     * "User silences phone every weekday at 9:00 AM"
     */
    fun injectMockDataAndRunAI(context: Context, eventRepository: EventRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Starting mock data injection...")
            
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            
            val events = mutableListOf<BehaviorEvent>()
            
            // Generate data for the past 21 days (3 weeks)
            for (dayOffset in 0..21) {
                calendar.timeInMillis = now - (dayOffset * 24 * 60 * 60 * 1000L)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()
                
                // Only on weekdays (Mon=1 ... Fri=5)
                if (dayOfWeek in 1..5) {
                    // Create a Silent Mode event around 9 AM (with slight variation)
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, (Math.random() * 15).toInt()) // 9:00 to 9:14
                    
                    val timestamp = calendar.timeInMillis
                    
                    events.add(
                        BehaviorEvent(
                            timestamp = timestamp,
                            eventType = BehaviorEvent.TYPE_SILENT_MODE,
                            payload = """{"ringerMode":"SILENT","previousMode":"NORMAL"}""",
                            dayOfWeek = dayOfWeek,
                            hourOfDay = 9,
                            isProcessed = false
                        )
                    )
                }
            }
            
            eventRepository.insertBehaviorEvents(events)
            Log.d(TAG, "Injected ${events.size} mock events.")
            
            // Trigger the AI Worker to process this new data immediately
            Log.d(TAG, "Triggering PatternAnalysisWorker...")
            val aiWork = OneTimeWorkRequestBuilder<PatternAnalysisWorker>()
                .addTag(PatternAnalysisWorker.WORK_NAME)
                .build()
                
            WorkManager.getInstance(context).enqueueUniqueWork(
                "manual_pattern_analysis",
                ExistingWorkPolicy.REPLACE,
                aiWork
            )
        }
    }
    
    private fun Int.toIsoDayOfWeek(): Int = when (this) {
        Calendar.SUNDAY -> 7
        else -> this - 1
    }
}
