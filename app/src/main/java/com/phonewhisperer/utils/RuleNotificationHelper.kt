package com.phonewhisperer.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.phonewhisperer.MainActivity
import com.phonewhisperer.R
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity

/**
 * Helper for showing notifications when AI rules are executed.
 *
 * Creates a persistent notification channel and posts notifications
 * so the user always knows when PhoneWhisperer has taken an action.
 */
object RuleNotificationHelper {

    private const val CHANNEL_ID = "phonewhisperer_rule_execution"
    private const val CHANNEL_NAME = "AI Rule Execution"
    private const val NOTIFICATION_ID_BASE = 5000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when PhoneWhisperer executes an automation rule"
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyRuleExecuted(context: Context, rule: AutomationRuleEntity) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (rule.actionType) {
            "RINGER_MODE" -> R.drawable.ic_launcher_foreground
            "DND" -> R.drawable.ic_launcher_foreground
            else -> R.drawable.ic_launcher_foreground
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🤖 ${rule.name}")
            .setContentText(rule.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(rule.description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((NOTIFICATION_ID_BASE + rule.id).toInt(), notification)
    }
}
