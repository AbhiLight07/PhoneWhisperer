package com.phonewhisperer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorPatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: BehaviorPatternEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patterns: List<BehaviorPatternEntity>): List<Long>

    @Query("SELECT * FROM behavior_patterns ORDER BY confidence DESC")
    fun getAllPatterns(): Flow<List<BehaviorPatternEntity>>

    @Query("DELETE FROM behavior_patterns")
    suspend fun deleteAll()
}
