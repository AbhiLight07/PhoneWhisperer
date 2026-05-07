package com.phonewhisperer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutomationRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<AutomationRuleEntity>): List<Long>

    @Update
    suspend fun update(rule: AutomationRuleEntity)

    @Query("SELECT * FROM automation_rules WHERE status = :status ORDER BY id DESC")
    fun getRulesByStatus(status: String): Flow<List<AutomationRuleEntity>>

    @Query("UPDATE automation_rules SET status = :status WHERE id = :ruleId")
    suspend fun updateStatus(ruleId: Long, status: String)

    @Query("DELETE FROM automation_rules")
    suspend fun deleteAll()
}
