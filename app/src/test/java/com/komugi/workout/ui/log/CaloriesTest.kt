package com.komugi.workout.ui.log

import com.komugi.workout.data.BodyMetricEntity
import com.komugi.workout.data.EntryEntity
import com.komugi.workout.data.EntryUi
import com.komugi.workout.data.SetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaloriesTest {

    private fun entry(
        parts: String?,
        restSec: String? = "60",
        sets: List<SetEntity>
    ) = EntryUi(
        entry = EntryEntity(id = 1, sessionId = 1, name = "測試", targetParts = parts, restSec = restSec),
        sets = sets
    )

    private fun repSet(reps: Int, weightKg: Double? = 60.0) =
        SetEntity(id = 1, entryId = 1, setIndex = 0, weightKg = weightKg, reps = reps)

    @Test
    fun metOrderedByIntensity() {
        val cardio = Calories.metForParts("股四頭、臀大肌、小腿、心肺")
        val legs = Calories.metForParts("股四頭、臀大肌、腿後腱、核心")
        val chest = Calories.metForParts("胸大肌、前三角、肱三頭")
        val core = Calories.metForParts("腹直肌、腹斜肌、腹橫肌")
        val arms = Calories.metForParts("肱二頭、肱肌、肱橈肌")
        val unknown = Calories.metForParts(null)

        assertTrue(cardio > legs)
        assertTrue(legs > chest)
        assertTrue(chest > core)
        assertTrue(core > arms)
        assertEquals(Calories.DEFAULT_MET, unknown, 0.0)
    }

    @Test
    fun timedSetUsesDurationNotReps() {
        val timed = SetEntity(id = 1, entryId = 1, setIndex = 0, durationSec = 45, reps = 99)
        assertEquals(45, Calories.workSeconds(timed))
        assertEquals(40, Calories.workSeconds(repSet(reps = 10)))
        assertEquals(0, Calories.workSeconds(SetEntity(id = 2, entryId = 1, setIndex = 1)))
    }

    @Test
    fun restOnlyCountsBetweenSets() {
        val three = entry("胸大肌", restSec = "60", sets = listOf(repSet(8), repSet(8), repSet(8)))
        val one = entry("胸大肌", restSec = "60", sets = listOf(repSet(8)))
        assertEquals(120, Calories.restSeconds(three))
        assertEquals(0, Calories.restSeconds(one))
        // 「直上」（0 秒）不計休息
        assertEquals(0, Calories.restSeconds(entry("胸大肌", restSec = "0", sets = listOf(repSet(8), repSet(8)))))
    }

    @Test
    fun kcalMatchesMetFormula() {
        // 3 組 × 10 下 × 4 秒 = 120 秒動作，休息 60 × 2 = 120 秒，體重 70 kg，胸 4.5 MET
        val e = entry("胸大肌", restSec = "60", sets = listOf(repSet(10), repSet(10), repSet(10)))
        val hours = 120.0 / 3600.0
        val expected = 4.5 * 70.0 * hours + Calories.REST_MET * 70.0 * hours
        assertEquals(expected, Calories.entryKcal(e, 70.0), 0.001)
    }

    @Test
    fun emptyEntryAndSessionCostNothing() {
        val empty = entry("胸大肌", sets = emptyList())
        assertEquals(0.0, Calories.entryKcal(empty, 70.0), 0.0)
        assertEquals(0.0, Calories.sessionKcal(listOf(empty), 70.0), 0.0)
        assertEquals(0, Calories.displayKcal(0.0))
        assertEquals(1, Calories.displayKcal(0.1))
        assertEquals(42, Calories.displayKcal(41.2))
    }

    @Test
    fun sessionSumsEntries() {
        val a = entry("胸大肌", restSec = "60", sets = listOf(repSet(10), repSet(10)))
        val b = entry("股四頭、臀大肌", restSec = "90", sets = listOf(repSet(10), repSet(10)))
        val sum = Calories.entryKcal(a, 70.0) + Calories.entryKcal(b, 70.0)
        assertEquals(sum, Calories.sessionKcal(listOf(a, b), 70.0), 0.001)
        assertTrue(Calories.entryKcal(b, 70.0) > Calories.entryKcal(a, 70.0))
    }

    @Test
    fun weightPicksNearestPreviousRecord() {
        val metrics = listOf(
            BodyMetricEntity(id = 1, dateEpochDay = 10, weightKg = 70.1),
            BodyMetricEntity(id = 2, dateEpochDay = 30, weightKg = 69.0),
            BodyMetricEntity(id = 3, dateEpochDay = 50, weightKg = null)
        )
        // 當天有記錄就用當天
        assertEquals(69.0, Calories.weightForDate(metrics, 30), 0.0)
        // 日期介於兩筆之間取較早那筆
        assertEquals(70.1, Calories.weightForDate(metrics, 20), 0.0)
        // 早於所有記錄取最早一筆
        assertEquals(70.1, Calories.weightForDate(metrics, 5), 0.0)
        // 晚於所有記錄取最近一筆（跳過無體重的）
        assertEquals(69.0, Calories.weightForDate(metrics, 999), 0.0)
        // 完全沒有體重時用備援值
        assertEquals(
            Calories.FALLBACK_WEIGHT_KG,
            Calories.weightForDate(emptyList(), 100),
            0.0
        )
        assertEquals(
            Calories.FALLBACK_WEIGHT_KG,
            Calories.weightForDate(listOf(BodyMetricEntity(dateEpochDay = 1, weightKg = null)), 1),
            0.0
        )
    }
}
