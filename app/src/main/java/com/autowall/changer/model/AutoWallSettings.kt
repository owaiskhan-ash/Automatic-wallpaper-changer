package com.autowall.changer.model

/** How the photo is mapped onto the screen. */
enum class FitMode {
    /** Crop the photo to fill the screen edge to edge. */
    FILL,

    /** Show the whole photo, with a soft blurred copy filling the leftover space. */
    FIT,
}

/** Optional colour wash applied over the wallpaper. */
data class Tone(val label: String, val argb: Long)

val TONES: List<Tone> = listOf(
    Tone("None", 0x00000000),
    Tone("Midnight", 0xFF0A1B3A),
    Tone("Dusk", 0xFF2A1636),
    Tone("Forest", 0xFF0C2A1E),
    Tone("Ember", 0xFF3A150F),
    Tone("Ocean", 0xFF04283A),
    Tone("Mono", 0xFF101014),
)

/** Intervals offered in the UI, in minutes. */
val INTERVALS: List<Int> = listOf(1, 5, 15, 30, 60, 180, 360, 720, 1440)

fun intervalLabel(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 && minutes < 1440 -> "${minutes / 60}h"
    minutes == 1440 -> "24h"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes}m"
}

data class AutoWallSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 60,
    val fitMode: FitMode = FitMode.FILL,
    val cornerRadiusDp: Int = 24,
    val toneIndex: Int = 0,
    val toneStrength: Float = 0.22f,
    val shuffle: Boolean = false,
    val applyToLockScreen: Boolean = true,
    val lastAppliedAt: Long = 0L,
    val lastIndex: Int = -1,
) {
    val tone: Tone get() = TONES.getOrElse(toneIndex) { TONES.first() }
}
