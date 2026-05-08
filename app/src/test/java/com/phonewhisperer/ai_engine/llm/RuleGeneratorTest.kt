package com.phonewhisperer.ai_engine.llm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RuleGeneratorTest {

    private lateinit var context: Context
    private lateinit var ruleGenerator: RuleGenerator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        ruleGenerator = RuleGenerator()
    }

    @Test
    fun `generateRules should fallback to heuristic when models are not available`() {
        val pattern = BehaviorPatternEntity(
            patternType = "TYPE_APP_USAGE",
            startHour = 22,
            endHour = 6,
            dayOfWeekMask = 127,
            confidence = 0.9f,
            eventCount = 20,
            associatedApps = "com.spotify.music"
        )

        val rules = ruleGenerator.generateRules(listOf(pattern), context)
        
        assertEquals(1, rules.size)
        val rule = rules[0]
        
        // The heuristic for late night should set DND to ALARMS or SILENT
        assertEquals("DND", rule.actionType)
        assertNotNull(rule.actionValue)
        assertEquals("TIME", rule.triggerType)
    }

    @Test
    fun `generateRules should generate work rule for daytime patterns`() {
        val pattern = BehaviorPatternEntity(
            patternType = "TYPE_GEOFENCE_TRANSITION",
            startHour = 9,
            endHour = 17,
            dayOfWeekMask = 31, // Mon-Fri
            confidence = 0.8f,
            eventCount = 10,
            associatedLocation = "Work"
        )

        val rules = ruleGenerator.generateRules(listOf(pattern), context)
        
        assertEquals(1, rules.size)
        val rule = rules[0]
        
        // Work heuristic usually sets RINGER_MODE to VIBRATE
        assertEquals("RINGER_MODE", rule.actionType)
        assertEquals("VIBRATE", rule.actionValue)
    }
}
