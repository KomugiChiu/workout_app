package com.komugi.workout.ui.menu

import com.komugi.workout.data.MenuExerciseRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleFlexTest {

    private var nextId = 1L

    private fun row(slot: String, sets: String = "3", name: String = slot): MenuExerciseRow =
        MenuExerciseRow(
            menuExerciseId = nextId++,
            menuId = CycleFlex.MENU_ID,
            exerciseId = nextId,
            orderIndex = 0,
            equipment = null,
            targetSets = sets,
            targetReps = "10",
            restSec = "60s",
            name = name,
            parts = null,
            videoUrl = null,
            guideId = null,
            supersetKey = null,
            slotKey = slot
        )

    private fun fullDraft(): List<MenuExerciseRow> = listOf(
        row(CycleFlex.SLOT_HEAD),
        row(CycleFlex.SLOT_T1),
        row(CycleFlex.SLOT_T2),
        row(CycleFlex.SLOT_T3),
        row(CycleFlex.SLOT_T4),
        row(CycleFlex.SLOT_T5),
        row(CycleFlex.SLOT_FINISH)
    )

    @Test
    fun dayAShowsTrunkPlusFinisher() {
        val slots = CycleFlex.applyDay(fullDraft(), CycleFlexDay.A, fullDraft())
            .map { it.slotKey }
        assertEquals(
            listOf("T1", "T2", "T3", "T4", "T5", "F"),
            slots
        )
    }

    @Test
    fun dayBShowsHeadPlusTrunk() {
        val slots = CycleFlex.applyDay(fullDraft(), CycleFlexDay.B, fullDraft())
            .map { it.slotKey }
        assertEquals(
            listOf("H", "T1", "T2", "T3", "T4", "T5"),
            slots
        )
    }

    @Test
    fun t4SetsFollowDay() {
        val template = fullDraft()
        val dayB = CycleFlex.applyDay(template, CycleFlexDay.B, template)
        assertEquals(
            CycleFlex.T4_SETS_DAY_B,
            dayB.first { it.slotKey == CycleFlex.SLOT_T4 }.targetSets
        )
        val dayA = CycleFlex.applyDay(dayB, CycleFlexDay.A, template)
        assertEquals("3", dayA.first { it.slotKey == CycleFlex.SLOT_T4 }.targetSets)
    }

    @Test
    fun foreignSlotsPassThrough() {
        val rows = listOf(row("1"), row("2A"), row("9"))
        assertEquals(rows, CycleFlex.applyDay(rows, CycleFlexDay.A, rows))
        assertEquals(rows, CycleFlex.applyDay(rows, CycleFlexDay.B, rows))
    }

    @Test
    fun dayRoundTripKeepsEdits() {
        val template = fullDraft()
        val edited = template.map {
            if (it.slotKey == CycleFlex.SLOT_T1) it.copy(exerciseId = 999L, name = "自訂硬舉")
            else it
        }
        val dayB = CycleFlex.applyDay(edited, CycleFlexDay.B, template)
        assertTrue(dayB.any { it.name == "自訂硬舉" })
        val backToA = CycleFlex.applyDay(dayB + edited.first { it.slotKey == "F" }, CycleFlexDay.A, template)
        assertTrue(backToA.any { it.name == "自訂硬舉" })
    }

    @Test
    fun storedDayRoundTrip() {
        assertEquals(CycleFlexDay.B, CycleFlex.dayFromStored("B"))
        assertEquals(CycleFlexDay.A, CycleFlex.dayFromStored("A"))
        assertEquals(CycleFlexDay.A, CycleFlex.dayFromStored(null))
        assertEquals("B", CycleFlex.dayToStored(CycleFlexDay.B))
    }
}
