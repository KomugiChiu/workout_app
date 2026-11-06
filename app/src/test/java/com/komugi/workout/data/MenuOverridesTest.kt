package com.komugi.workout.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuOverridesTest {

    private fun row(
        menuExerciseId: Long,
        slotKey: String?,
        exerciseId: Long,
        name: String
    ) = MenuExerciseRow(
        menuExerciseId = menuExerciseId,
        menuId = "plan-flex",
        exerciseId = exerciseId,
        orderIndex = 0,
        equipment = "啞鈴",
        targetSets = "3",
        targetReps = "12",
        restSec = "60",
        name = name,
        parts = "測試部位",
        videoUrl = null,
        guideId = null,
        supersetKey = null,
        slotKey = slotKey
    )

    private fun exercise(id: Long, name: String) = ExerciseEntity(
        id = id,
        name = name,
        parts = "測試部位",
        equipment = "啞鈴"
    )

    private val template = listOf(
        row(101, "1", 1, "平板支撐"),
        row(102, "2A", 2, "單手壺鈴 RDL")
    )

    @Test
    fun resolveMatchesBySlotKeyAfterReseed() {
        // 種子重建後 menuExerciseId 改變（101→201），slotKey 不變，記憶仍要生效
        val reseeded = listOf(
            row(201, "1", 1, "平板支撐"),
            row(202, "2A", 2, "單手壺鈴 RDL")
        )
        val overrides = listOf(
            MenuExerciseLastUsed(menuExerciseId = 102, exerciseId = 9, slotKey = "2A")
        )
        val applied = resolveExerciseOverrides(reseeded, overrides) {
            if (it == 9L) exercise(9, "單腳啞鈴 RDL") else null
        }

        assertEquals("平板支撐", applied[0].name)
        assertEquals("單腳啞鈴 RDL", applied[1].name)
        assertEquals(9L, applied[1].exerciseId)
    }

    @Test
    fun resolveIgnoresDanglingOverrides() {
        val overrides = listOf(
            MenuExerciseLastUsed(menuExerciseId = 999, exerciseId = 9, slotKey = "9Z"),
            MenuExerciseLastUsed(menuExerciseId = 998, exerciseId = 9),
            MenuExerciseLastUsed(menuExerciseId = 101, exerciseId = 404)
        )
        val applied = resolveExerciseOverrides(template, overrides) { null }

        assertEquals(template, applied)
    }

    @Test
    fun resolveSupportsLegacyRowIdOverrides() {
        val overrides = listOf(
            MenuExerciseLastUsed(menuExerciseId = 102, exerciseId = 9)
        )
        val applied = resolveExerciseOverrides(template, overrides) {
            if (it == 9L) exercise(9, "單腳啞鈴 RDL") else null
        }

        assertEquals("單腳啞鈴 RDL", applied[1].name)
        assertEquals("平板支撐", applied[0].name)
    }

    @Test
    fun diffKeepsOnlyChangedSlots() {
        val current = listOf(
            row(201, "1", 1, "平板支撐"),
            row(202, "2A", 9, "單腳啞鈴 RDL")
        )
        val diff = diffExerciseOverrides(current, template)

        assertEquals(1, diff.size)
        assertEquals("2A", diff[0].slotKey)
        assertEquals(9L, diff[0].exerciseId)
    }

    @Test
    fun diffIsEmptyWhenNothingChanged() {
        assertTrue(diffExerciseOverrides(template, template).isEmpty())
    }
}
