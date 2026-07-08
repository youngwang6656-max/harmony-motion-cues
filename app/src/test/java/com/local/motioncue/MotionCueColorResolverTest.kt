package com.local.motioncue

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionCueColorResolverTest {
    @Test
    fun autoModeUsesWhiteDotsForDarkInterfaces() {
        assertEquals(CueDotColor.WHITE, MotionCueColorResolver.resolve(CueColorMode.AUTO, isNightMode = true))
    }

    @Test
    fun autoModeUsesBlackDotsForLightInterfaces() {
        assertEquals(CueDotColor.BLACK, MotionCueColorResolver.resolve(CueColorMode.AUTO, isNightMode = false))
    }

    @Test
    fun explicitColorModesIgnoreInterfaceMode() {
        assertEquals(CueDotColor.WHITE, MotionCueColorResolver.resolve(CueColorMode.WHITE, isNightMode = false))
        assertEquals(CueDotColor.BLACK, MotionCueColorResolver.resolve(CueColorMode.BLACK, isNightMode = true))
    }
}
