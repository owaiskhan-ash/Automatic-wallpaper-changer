package com.autowall.changer

import com.autowall.changer.model.FitMode
import com.autowall.changer.model.INTERVALS
import com.autowall.changer.model.TONES
import com.autowall.changer.model.intervalLabel
import com.autowall.changer.work.RotationPicker
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationLogicTest {

    @Test
    fun `sequential rotation advances and wraps`() {
        assertEquals(0, RotationPicker.nextIndex(last = -1, size = 3, shuffle = false))
        assertEquals(1, RotationPicker.nextIndex(last = 0, size = 3, shuffle = false))
        assertEquals(2, RotationPicker.nextIndex(last = 1, size = 3, shuffle = false))
        assertEquals(0, RotationPicker.nextIndex(last = 2, size = 3, shuffle = false))
    }

    @Test
    fun `rotation is safe with a single photo`() {
        assertEquals(0, RotationPicker.nextIndex(last = 0, size = 1, shuffle = false))
        assertEquals(0, RotationPicker.nextIndex(last = 0, size = 1, shuffle = true))
        assertEquals(0, RotationPicker.nextIndex(last = 9, size = 0, shuffle = true))
    }

    @Test
    fun `shuffle never repeats the current photo`() {
        val random = Random(42)
        repeat(200) {
            val next = RotationPicker.nextIndex(last = 2, size = 5, shuffle = true, random = random)
            assertNotEquals(2, next)
            assertTrue(next in 0..4)
        }
    }

    @Test
    fun `shuffle covers every photo`() {
        val random = Random(7)
        val seen = mutableSetOf<Int>()
        repeat(500) { seen += RotationPicker.nextIndex(last = -1, size = 4, shuffle = true, random = random) }
        assertEquals(setOf(0, 1, 2, 3), seen)
    }

    @Test
    fun `one minute is offered as the shortest rotation interval`() {
        assertEquals(1, INTERVALS.first())
        assertEquals("1m", intervalLabel(INTERVALS.first()))
    }

    @Test
    fun `interval labels read naturally`() {
        assertEquals("1m", intervalLabel(1))
        assertEquals("5m", intervalLabel(5))
        assertEquals("30m", intervalLabel(30))
        assertEquals("1h", intervalLabel(60))
        assertEquals("12h", intervalLabel(720))
        assertEquals("24h", intervalLabel(1440))
    }

    @Test
    fun `tones include the neutral option first and are all opaque or clear`() {
        assertEquals("None", TONES.first().label)
        assertEquals(0L, TONES.first().argb)
        assertTrue(TONES.size >= 5)
        assertTrue(TONES.drop(1).all { it.argb != 0L })
    }

    @Test
    fun `fit modes survive a round trip through their name`() {
        FitMode.entries.forEach { mode ->
            assertEquals(mode, FitMode.valueOf(mode.name))
        }
    }
}
