package com.phonewhisperer.di

import com.phonewhisperer.data.repository.EventRepository
import com.phonewhisperer.data.repository.EventRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings.
 *
 * Uses @Binds (more efficient than @Provides for interface→impl mappings)
 * to bind EventRepository interface to its implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
}
