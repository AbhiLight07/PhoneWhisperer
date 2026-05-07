package com.phonewhisperer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phonewhisperer.data.local.db.dao.AppUsageEventDao
import com.phonewhisperer.data.local.db.dao.AutomationRuleDao
import com.phonewhisperer.data.local.db.dao.BehaviorEventDao
import com.phonewhisperer.data.local.db.dao.BehaviorPatternDao
import com.phonewhisperer.data.local.db.dao.LocationEventDao
import com.phonewhisperer.data.local.db.dao.NotificationEventDao
import com.phonewhisperer.data.local.db.entity.AppUsageEvent
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import com.phonewhisperer.data.local.db.entity.LocationEvent
import com.phonewhisperer.data.local.db.entity.NotificationEvent

/**
 * PhoneWhisperer Room Database.
 *
 * Version history:
 *   v1 — Phase 1: behavior_events, location_events, app_usage_events
 *   v2 — Phase 2: + notification_events table
 *   v3 — Phase 3: + behavior_patterns, automation_rules tables (AI Engine)
 */
@Database(
    entities = [
        BehaviorEvent::class,
        LocationEvent::class,
        AppUsageEvent::class,
        NotificationEvent::class,
        BehaviorPatternEntity::class,
        AutomationRuleEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun behaviorEventDao(): BehaviorEventDao
    abstract fun locationEventDao(): LocationEventDao
    abstract fun appUsageEventDao(): AppUsageEventDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun behaviorPatternDao(): BehaviorPatternDao
    abstract fun automationRuleDao(): AutomationRuleDao

    companion object {
        const val DATABASE_NAME = "phonewhisperer_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notification_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `package_name` TEXT NOT NULL,
                        `app_name` TEXT NOT NULL,
                        `category` TEXT,
                        `priority` INTEGER NOT NULL DEFAULT 0,
                        `action` TEXT NOT NULL,
                        `day_of_week` INTEGER NOT NULL,
                        `hour_of_day` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_events_package_name` ON `notification_events` (`package_name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_events_timestamp` ON `notification_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_events_day_of_week_hour_of_day` ON `notification_events` (`day_of_week`, `hour_of_day`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_events_action` ON `notification_events` (`action`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Behavior Patterns
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `behavior_patterns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cluster_id` INTEGER NOT NULL,
                        `pattern_type` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `day_of_week_mask` INTEGER NOT NULL,
                        `start_hour` INTEGER NOT NULL,
                        `end_hour` INTEGER NOT NULL,
                        `associated_apps` TEXT NOT NULL,
                        `associated_location` TEXT,
                        `event_count` INTEGER NOT NULL,
                        `first_seen` INTEGER NOT NULL,
                        `last_seen` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Automation Rules
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `automation_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `pattern_id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `trigger_type` TEXT NOT NULL,
                        `trigger_value` TEXT NOT NULL,
                        `action_type` TEXT NOT NULL,
                        `action_value` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        FOREIGN KEY(`pattern_id`) REFERENCES `behavior_patterns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_automation_rules_pattern_id` ON `automation_rules` (`pattern_id`)")
            }
        }
    }
}
