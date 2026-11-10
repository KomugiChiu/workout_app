package com.komugi.workout.ui.log

import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerTest {

    @Test
    fun remainingCountsDownByWallClock() {
        val end = 60_000L
        assertEquals(60, remainingSecondsAt(end, 0L))
        assertEquals(59, remainingSecondsAt(end, 1_000L))
        assertEquals(30, remainingSecondsAt(end, 30_000L))
        assertEquals(1, remainingSecondsAt(end, 59_000L))
        assertEquals(1, remainingSecondsAt(end, 59_500L))
        assertEquals(0, remainingSecondsAt(end, 60_000L))
        assertEquals(0, remainingSecondsAt(end, 75_000L))
    }

    @Test
    fun noTimerMeansZero() {
        assertEquals(0, remainingSecondsAt(0L, 1_000L))
        assertEquals(0, remainingSecondsAt(-5L, 1_000L))
    }

    @Test
    fun backgroundGapIsReflectedOnReturn() {
        // 開始 60 秒，切到別的頁面 25 秒後回來：剩 35 秒，而不是從 60 重新開始
        val started = 1_000_000L
        val end = started + 60_000L
        assertEquals(35, remainingSecondsAt(end, started + 25_000L))
        // 螢幕休眠 10 分鐘後回來也不會殘留舊值
        assertEquals(0, remainingSecondsAt(end, started + 600_000L))
    }

    @Test
    fun clockTextFormatsMinutesAndHours() {
        assertEquals("0:00", formatClockText(0))
        assertEquals("0:05", formatClockText(5))
        assertEquals("1:00", formatClockText(60))
        assertEquals("9:59", formatClockText(599))
        assertEquals("1:00:00", formatClockText(3600))
        assertEquals("2:05:09", formatClockText(7509))
        // 負數不會出現負號
        assertEquals("0:00", formatClockText(-3))
    }
}
