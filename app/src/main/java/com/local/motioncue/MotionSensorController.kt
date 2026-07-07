package com.local.motioncue

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

class MotionSensorController(
    context: Context,
    private val configProvider: () -> MotionCueConfig,
    private val onState: (MotionCueState) -> Unit
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val filter = MotionSignalFilter(configProvider)
    private var gravity = FloatArray(3)
    private var latestYawRate = 0f
    private var running = false

    fun start() {
        if (running) return
        running = true
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
        gravity = FloatArray(3)
        latestYawRate = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> latestYawRate = event.values[2]
            Sensor.TYPE_LINEAR_ACCELERATION -> handleLinearAcceleration(event)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun handleAccelerometer(event: SensorEvent) {
        val gravityAlpha = 0.88f
        gravity[0] = gravityAlpha * gravity[0] + (1f - gravityAlpha) * event.values[0]
        gravity[1] = gravityAlpha * gravity[1] + (1f - gravityAlpha) * event.values[1]
        gravity[2] = gravityAlpha * gravity[2] + (1f - gravityAlpha) * event.values[2]

        val linearX = (event.values[0] - gravity[0]) / SensorManager.GRAVITY_EARTH
        val linearY = (event.values[1] - gravity[1]) / SensorManager.GRAVITY_EARTH
        val rotated = rotateToDisplay(linearX, linearY)
        onState(filter.update(rotated.first, rotated.second, latestYawRate, event.timestamp))
    }

    private fun handleLinearAcceleration(event: SensorEvent) {
        val linearX = event.values[0] / SensorManager.GRAVITY_EARTH
        val linearY = event.values[1] / SensorManager.GRAVITY_EARTH
        val rotated = rotateToDisplay(linearX, linearY)
        onState(filter.update(rotated.first, rotated.second, latestYawRate, event.timestamp))
    }

    @Suppress("DEPRECATION")
    private fun rotateToDisplay(x: Float, y: Float): Pair<Float, Float> {
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> Pair(-y, x)
            Surface.ROTATION_180 -> Pair(-x, -y)
            Surface.ROTATION_270 -> Pair(y, -x)
            else -> Pair(x, y)
        }
    }
}

class MotionSignalFilter(private val configProvider: () -> MotionCueConfig) {
    private var lastTimestampNanos: Long = 0L
    private var lateral = 0f
    private var longitudinal = 0f
    private var rotation = 0f

    fun update(
        rawLateralG: Float,
        rawLongitudinalG: Float,
        rawYawRadPerSec: Float,
        timestampNanos: Long
    ): MotionCueState {
        val config = configProvider()
        val dtSeconds = if (lastTimestampNanos == 0L) {
            0.02f
        } else {
            ((timestampNanos - lastTimestampNanos).coerceAtLeast(1L) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        }
        lastTimestampNanos = timestampNanos

        val targetLateral = normalizeAxis(rawLateralG, config)
        val targetLongitudinal = normalizeAxis(rawLongitudinalG, config)
        val targetRotation = (rawYawRadPerSec * 0.35f * config.sensitivity).coerceIn(-1f, 1f)

        lateral = approach(lateral, targetLateral, dtSeconds, config)
        longitudinal = approach(longitudinal, targetLongitudinal, dtSeconds, config)
        rotation = approach(rotation, targetRotation, dtSeconds, config)

        val confidence = max(abs(lateral), abs(longitudinal)).coerceIn(0f, 1f)
        return MotionCueState(lateral, longitudinal, rotation, confidence)
    }

    private fun normalizeAxis(rawG: Float, config: MotionCueConfig): Float {
        val magnitude = abs(rawG)
        if (magnitude < config.deadZoneG) return 0f
        val signed = rawG.sign() * (magnitude - config.deadZoneG)
        return (signed * 5.0f * config.sensitivity).coerceIn(-1f, 1f)
    }

    private fun approach(current: Float, target: Float, dtSeconds: Float, config: MotionCueConfig): Float {
        val timeMs = if (abs(target) > abs(current)) config.attackTimeMs else config.releaseTimeMs
        val alpha = 1f - exp((-dtSeconds * 1000f / timeMs.coerceAtLeast(1)).toDouble()).toFloat()
        val next = current + (target - current) * alpha
        return if (abs(next) < 0.001f) 0f else next.coerceIn(-1f, 1f)
    }

    private fun Float.sign(): Float = if (this < 0f) -1f else 1f
}
