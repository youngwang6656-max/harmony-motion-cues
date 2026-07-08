package com.local.motioncue

enum class CueDotColor(val argb: Int) {
    WHITE(0xFFFFFFFF.toInt()),
    BLACK(0xFF000000.toInt())
}

object MotionCueColorResolver {
    fun resolve(mode: CueColorMode, isNightMode: Boolean): CueDotColor {
        return when (mode) {
            CueColorMode.WHITE -> CueDotColor.WHITE
            CueColorMode.BLACK -> CueDotColor.BLACK
            CueColorMode.AUTO -> if (isNightMode) CueDotColor.WHITE else CueDotColor.BLACK
        }
    }
}
