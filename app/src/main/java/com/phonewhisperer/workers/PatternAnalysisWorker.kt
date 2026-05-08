package com.phonewhisperer.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.domain.usecase.AnalyzePatternsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that runs the AI pattern analysis pipeline.
 *
 * Phase 3 implementation. Scheduled to run nightly or triggered manually via Debug.
 * Pipeline:
 * 1. Fetch unprocessed BehaviorEvents from Room
 * 2. Group events by type
 * 3. Run DBSCAN clustering on each type
 * 4. Save detected BehaviorPatterns to DB
 * 5. Feed patterns to RuleGenerator (LLM)
 * 6. Store proposed AutomationRules for user approval
 * 7. Mark original events as processed
 */
@HiltWorker
class PatternAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val analyzePatternsUseCase: AnalyzePatternsUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "PatternAnalysisWorker"
        const val WORK_NAME = "phonewhisperer_pattern_analysis"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Pattern analysis worker starting...")

        val result = analyzePatternsUseCase.invoke()
        return if (result.isSuccess) {
            val stats = result.getOrNull()
            Log.d(TAG, "Pattern analysis complete. Found ${stats?.patternsFound} patterns, generated ${stats?.rulesGenerated} rules.")
            Result.success()
        } else {
            Result.failure()
        }
    }
}
