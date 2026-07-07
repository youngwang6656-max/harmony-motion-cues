package com.local.motioncue

data class CueDot(
    val baseX: Float,
    val baseY: Float,
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

class MotionCueModel {
    fun dots(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        state: MotionCueState,
        config: MotionCueConfig
    ): List<CueDot> {
        if (widthPx <= 0 || heightPx <= 0) return emptyList()
        val radius = config.dotRadiusDp * density
        val inset = config.edgeInsetDp * density
        val maxMove = config.maxDisplacementDp * density
        val horizontalCount = 7
        val verticalCount = 7
        val dots = mutableListOf<CueDot>()

        fun addDot(baseX: Float, baseY: Float, edgeFactorX: Float, edgeFactorY: Float, cornerFactor: Float) {
            val dx = state.lateral * maxMove * edgeFactorX + state.rotation * maxMove * 0.25f * cornerFactor
            val dy = state.longitudinal * maxMove * edgeFactorY - state.rotation * maxMove * 0.18f * cornerFactor
            dots += CueDot(
                baseX = baseX,
                baseY = baseY,
                x = baseX + dx,
                y = baseY + dy,
                radius = radius,
                alpha = config.opacity.coerceIn(0f, 1f)
            )
        }

        repeat(verticalCount) { index ->
            val t = (index + 1f) / (verticalCount + 1f)
            val y = heightPx * t
            val corner = (t - 0.5f) * 2f
            addDot(inset, y, 1f, 0.25f, corner)
            addDot(widthPx - inset, y, 1f, 0.25f, -corner)
        }

        repeat(horizontalCount) { index ->
            val t = (index + 1f) / (horizontalCount + 1f)
            val x = widthPx * t
            val corner = (t - 0.5f) * 2f
            addDot(x, inset, 0.25f, 1f, -corner)
            addDot(x, heightPx - inset, 0.25f, 1f, corner)
        }

        return dots
    }
}
