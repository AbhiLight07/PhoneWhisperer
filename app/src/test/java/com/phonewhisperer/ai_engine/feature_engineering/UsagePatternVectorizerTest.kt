package com.phonewhisperer.ai_engine.feature_engineering

import org.junit.Assert.assertEquals
import org.junit.Test

class UsagePatternVectorizerTest {

    @Test
    fun `getCategory should correctly map known packages`() {
        assertEquals("MUSIC", UsagePatternVectorizer.getCategory("com.spotify.music"))
        assertEquals("SOCIAL", UsagePatternVectorizer.getCategory("com.whatsapp"))
        assertEquals("SOCIAL", UsagePatternVectorizer.getCategory("com.instagram.android"))
        assertEquals("WORK", UsagePatternVectorizer.getCategory("com.slack"))
    }

    @Test
    fun `getCategory should return UNKNOWN for unknown packages`() {
        assertEquals("UNKNOWN", UsagePatternVectorizer.getCategory("com.some.random.app"))
    }
}
