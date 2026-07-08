package com.local.motioncue

data class MotionCueState(
    val lateral: Float,
    val longitudinal: Float,
    val rotation: Float,
    val confidence: Float
) {
    companion object {
        val Zero = MotionCueState(0f, 0f, 0f, 0f)
    }
}

enum class CueColorMode {
    WHITE,
    BLACK,
    AUTO
}

data class MotionCueConfig(
    val dotRadiusDp: Float = 5f,
    val edgeInsetDp: Float = 18f,
    val maxDisplacementDp: Float = 32f,
    val opacity: Float = 0.75f,
    val sensitivity: Float = 1f,
    val deadZoneG: Float = 0.035f,
    val attackTimeMs: Int = 90,
    val releaseTimeMs: Int = 420,
    val manualAlwaysOn: Boolean = true,
    val colorMode: CueColorMode = CueColorMode.AUTO
)
