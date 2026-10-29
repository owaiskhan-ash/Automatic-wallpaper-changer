package com.autowall.changer.work

import kotlin.random.Random

/** Pure rotation-order logic, kept free of Android types so it can be unit tested. */
object RotationPicker {

    fun nextIndex(
        last: Int,
        size: Int,
        shuffle: Boolean,
        random: Random = Random.Default,
    ): Int {
        if (size <= 1) return 0
        if (shuffle) {
            var candidate = random.nextInt(size)
            if (candidate == last) candidate = (candidate + 1) % size
            return candidate
        }
        return (last + 1).mod(size)
    }
}
