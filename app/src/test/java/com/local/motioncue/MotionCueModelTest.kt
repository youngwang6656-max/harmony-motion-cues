package com.local.motioncue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionCueModelTest {
    @Test
    fun createsDotsOnAllEdges() {
        val dots = MotionCueModel().dots(
            widthPx = 1080,
            heightPx = 2400,
            density = 3f,
            state = MotionCueState.Zero,
            config = MotionCueConfig()
        )
        assertEquals(28, dots.size)
    }

    @Test
    fun lateralMotionMovesSideDots() {
        val model = MotionCueModel()
        val neutral = model.dots(1080, 2400, 3f, MotionCueState.Zero, MotionCueConfig())
        val moved = model.dots(
            1080,
            2400,
            3f,
            MotionCueState(lateral = 1f, longitudinal = 0f, rotation = 0f, confidence = 1f),
            MotionCueConfig()
        )
        assertTrue(moved.first().x > neutral.first().x)
    }

    @Test
    fun opacityIsAppliedToAllDots() {
        val dots = MotionCueModel().dots(1080, 2400, 3f, MotionCueState.Zero, MotionCueConfig(opacity = 0.6f))
        assertTrue(dots.all { it.alpha == 0.6f })
    }
}
