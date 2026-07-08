package com.local.motioncue

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class MotionCueOverlayView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val model = MotionCueModel()
    private var state = MotionCueState.Zero
    private var config = MotionCueConfig()

    init {
        setWillNotDraw(false)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun updateState(next: MotionCueState) {
        state = next
        postInvalidateOnAnimation()
    }

    fun updateConfig(next: MotionCueConfig) {
        config = next
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        paint.color = MotionCueColorResolver.resolve(config.colorMode, isNightMode).argb
        val dots = model.dots(width, height, resources.displayMetrics.density, state, config)
        for (dot in dots) {
            paint.alpha = (dot.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(dot.x, dot.y, dot.radius, paint)
        }
    }
}
