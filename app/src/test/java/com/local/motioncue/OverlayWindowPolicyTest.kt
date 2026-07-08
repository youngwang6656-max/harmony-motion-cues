package com.local.motioncue

import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayWindowPolicyTest {
    @Test
    fun touchPassThroughAlphaStaysBelowSystemObscuringLimit() {
        assertTrue(OverlayWindowPolicy.TOUCH_PASS_THROUGH_ALPHA < OverlayWindowPolicy.MAX_OBSCURING_ALPHA)
    }

    @Test
    fun touchPassThroughAlphaKeepsDotsVisible() {
        assertTrue(OverlayWindowPolicy.TOUCH_PASS_THROUGH_ALPHA >= 0.5f)
    }
}
