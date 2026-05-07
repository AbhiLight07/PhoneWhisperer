package com.phonewhisperer.di

import com.phonewhisperer.data.repository.EventRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun eventRepository(): EventRepository
}
