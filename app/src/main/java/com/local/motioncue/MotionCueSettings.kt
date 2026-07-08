package com.local.motioncue

import android.content.Context

class MotionCueSettings(context: Context) {
    private val prefs = context.getSharedPreferences("motion_cue_settings", Context.MODE_PRIVATE)

    fun load(): MotionCueConfig {
        val modeName = prefs.getString("colorMode", CueColorMode.AUTO.name) ?: CueColorMode.AUTO.name
        val mode = runCatching { CueColorMode.valueOf(modeName) }.getOrDefault(CueColorMode.AUTO)
        return MotionCueConfig(
            dotRadiusDp = prefs.getFloat("dotRadiusDp", 5f),
            edgeInsetDp = prefs.getFloat("edgeInsetDp", 18f),
            maxDisplacementDp = prefs.getFloat("maxDisplacementDp", 32f),
            opacity = prefs.getFloat("opacity", 0.75f),
            sensitivity = prefs.getFloat("sensitivity", 1f),
            deadZoneG = prefs.getFloat("deadZoneG", 0.035f),
            attackTimeMs = prefs.getInt("attackTimeMs", 90),
            releaseTimeMs = prefs.getInt("releaseTimeMs", 420),
            manualAlwaysOn = prefs.getBoolean("manualAlwaysOn", true),
            colorMode = mode
        )
    }

    fun save(config: MotionCueConfig) {
        prefs.edit()
            .putFloat("dotRadiusDp", config.dotRadiusDp)
            .putFloat("edgeInsetDp", config.edgeInsetDp)
            .putFloat("maxDisplacementDp", config.maxDisplacementDp)
            .putFloat("opacity", config.opacity)
            .putFloat("sensitivity", config.sensitivity)
            .putFloat("deadZoneG", config.deadZoneG)
            .putInt("attackTimeMs", config.attackTimeMs)
            .putInt("releaseTimeMs", config.releaseTimeMs)
            .putBoolean("manualAlwaysOn", config.manualAlwaysOn)
            .putString("colorMode", config.colorMode.name)
            .apply()
    }
}
