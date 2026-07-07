package com.local.motioncue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSensorFilterTest {
    private val config = MotionCueConfig()

    @Test
    fun deadZoneSuppressesTinyMotion() {
        val filter = MotionSignalFilter { config.copy(deadZoneG = 0.05f) }
        val state = filter.update(0.02f, -0.01f, 0.0f, 20_000_000L)
        assertEquals(0.0f, state.lateral, 0.0001f)
        assertEquals(0.0f, state.longitudinal, 0.0001f)
        assertEquals(0.0f, state.confidence, 0.0001f)
    }

    @Test
    fun strongMotionRisesQuicklyAndClamps() {
        val filter = MotionSignalFilter { config.copy(sensitivity = 3.0f, deadZoneG = 0.01f) }
        var state = MotionCueState.Zero
        repeat(10) { index ->
            state = filter.update(0.5f, -0.5f, 0.0f, (index + 1) * 20_000_000L)
        }
        assertTrue(state.lateral > 0.5f)
        assertTrue(state.longitudinal < -0.5f)
        assertTrue(state.lateral <= 1.0f)
        assertTrue(state.longitudinal >= -1.0f)
    }

    @Test
    fun releaseIsSlowerThanAttack() {
        val filter = MotionSignalFilter { config.copy(deadZoneG = 0.01f, attackTimeMs = 80, releaseTimeMs = 600) }
        repeat(8) { index ->
            filter.update(0.25f, 0.0f, 0.0f, (index + 1) * 20_000_000L)
        }
        val beforeRelease = filter.update(0.25f, 0.0f, 0.0f, 200_000_000L)
        val afterRelease = filter.update(0.0f, 0.0f, 0.0f, 220_000_000L)
        assertTrue(afterRelease.lateral > beforeRelease.lateral * 0.75f)
    }
}
