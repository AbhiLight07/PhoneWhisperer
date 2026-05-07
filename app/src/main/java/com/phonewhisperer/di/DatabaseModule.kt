package com.phonewhisperer.di

import android.content.Context
import androidx.room.Room
import com.phonewhisperer.data.local.db.AppDatabase
import com.phonewhisperer.data.local.db.dao.AppUsageEventDao
import com.phonewhisperer.data.local.db.dao.AutomationRuleDao
import com.phonewhisperer.data.local.db.dao.BehaviorEventDao
import com.phonewhisperer.data.local.db.dao.BehaviorPatternDao
import com.phonewhisperer.data.local.db.dao.LocationEventDao
import com.phonewhisperer.data.local.db.dao.NotificationEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideBehaviorEventDao(database: AppDatabase): BehaviorEventDao = database.behaviorEventDao()

    @Provides
    @Singleton
    fun provideLocationEventDao(database: AppDatabase): LocationEventDao = database.locationEventDao()

    @Provides
    @Singleton
    fun provideAppUsageEventDao(database: AppDatabase): AppUsageEventDao = database.appUsageEventDao()

    @Provides
    @Singleton
    fun provideNotificationEventDao(database: AppDatabase): NotificationEventDao = database.notificationEventDao()

    @Provides
    @Singleton
    fun provideBehaviorPatternDao(database: AppDatabase): BehaviorPatternDao = database.behaviorPatternDao()

    @Provides
    @Singleton
    fun provideAutomationRuleDao(database: AppDatabase): AutomationRuleDao = database.automationRuleDao()
}
