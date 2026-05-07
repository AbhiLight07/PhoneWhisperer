package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.di.RepositoryEntryPoint
import com.phonewhisperer.execution.ActionExecutor
import com.phonewhisperer.execution.TimeRuleScheduler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Catches exact alarms fired by [TimeRuleScheduler] and executes the rule action.
 *
 * Pipeline: AlarmManager → RuleExecutionReceiver → ActionExecutor
 *
 * After executing:
 *   1. Logs the execution as a BehaviorEvent for auditing
 *   2. Reschedules the alarm for the next day
 */
@AndroidEntryPoint
class RuleExecutionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var actionExecutor: ActionExecutor

    @Inject
    lateinit var timeRuleScheduler: TimeRuleScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "RuleExecutionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getLongExtra(TimeRuleScheduler.EXTRA_RULE_ID, -1L)
        if (ruleId == -1L) {
            Log.e(TAG, "Received alarm without rule ID")
            return
        }

        Log.d(TAG, "⏰ Alarm fired for rule ID: $ruleId")

        // Use the pending result to keep the receiver alive during async work
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Fetch the rule from the repository
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    RepositoryEntryPoint::class.java
                )
                val repository = entryPoint.eventRepository()

                // Get all approved rules and find ours
                val approvedRules = repository.getApprovedRules().first()
                val rule = approvedRules.find { it.id == ruleId }

                if (rule == null) {
                    Log.w(TAG, "Rule $ruleId no longer exists or is not approved — skipping")
                    return@launch
                }

                if (rule.status != AutomationRuleEntity.STATUS_APPROVED) {
                    Log.w(TAG, "Rule ${rule.name} is no longer approved — skipping")
                    return@launch
                }

                // Execute the action
                val success = actionExecutor.execute(rule)

                if (success) {
                    Log.d(TAG, "✓ Rule '${rule.name}' executed successfully")

                    // Notify the user
                    com.phonewhisperer.utils.RuleNotificationHelper.notifyRuleExecuted(context, rule)

                    // Log execution as a BehaviorEvent for auditing
                    val auditEvent = com.phonewhisperer.data.local.db.entity.BehaviorEvent(
                        timestamp = System.currentTimeMillis(),
                        eventType = "RULE_EXECUTED",
                        payload = """{"ruleId":${rule.id},"ruleName":"${rule.name}","actionType":"${rule.actionType}","actionValue":"${rule.actionValue}"}""",
                        dayOfWeek = java.util.Calendar.getInstance().let {
                            val dow = it.get(java.util.Calendar.DAY_OF_WEEK)
                            if (dow == java.util.Calendar.SUNDAY) 7 else dow - 1
                        },
                        hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    )
                    repository.insertBehaviorEvent(auditEvent)
                }

                // Reschedule for tomorrow
                timeRuleScheduler.rescheduleForNextDay(rule)

            } catch (e: Exception) {
                Log.e(TAG, "Error executing rule $ruleId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
