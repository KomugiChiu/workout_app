package com.komugi.workout.ui.menu

import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.MenuExerciseRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanFlexTest {

    private var nextId = 100L

    private fun row(
        slot: String,
        name: String = slot,
        role: String? = "角色",
        sets: String = "3"
    ): MenuExerciseRow = MenuExerciseRow(
        menuExerciseId = nextId++,
        menuId = PlanFlex.MENU_ID,
        exerciseId = nextId,
        orderIndex = 0,
        equipment = null,
        targetSets = sets,
        targetReps = "12",
        restSec = "60",
        name = name,
        parts = null,
        videoUrl = null,
        guideId = null,
        supersetKey = null,
        slotKey = slot,
        role = role,
        laterality = "通用",
        pattern = null
    )

    @Test
    fun dayBCoversAllSlots() {
        assertEquals(
            listOf("1", "2A", "2B", "3A", "3B"),
            PlanFlex.slots.sorted().let { PlanFlex.dayBDefaults.keys.sorted() }
        )
        assertEquals(
            listOf("鳥狗式", "保加利亞分腿蹲", "滑輪下拉", "啞鈴 RDL", "阿諾德推舉"),
            listOf("1", "2A", "2B", "3A", "3B").map { PlanFlex.dayBDefaults[it]!!.exerciseName }
        )
    }

    @Test
    fun dayAUsesTemplateAsIs() {
        val template = PlanFlex.slots.map { row(it) }
        assertEquals(template, PlanFlex.templateFor(template, PlanFlexDay.A) { null })
    }

    @Test
    fun dayBSwapsExerciseAndRoleButKeepsSetsRest() {
        val template = PlanFlex.slots.map { row(it, name = "A動作$it", role = "A角色") }
        val applied = PlanFlex.templateFor(template, PlanFlexDay.B) { n ->
            ExerciseEntity(
                id = 999L,
                name = n,
                parts = "測試部位",
                equipment = "測試器材"
            )
        }
        assertEquals(template.map { it.menuExerciseId }, applied.map { it.menuExerciseId })
        assertEquals(template.map { it.targetSets }, applied.map { it.targetSets })
        assertEquals(template.map { it.restSec }, applied.map { it.restSec })
        val slot2B = applied.first { it.slotKey == "2B" }
        assertEquals("滑輪下拉", slot2B.name)
        assertEquals("背闊肌", slot2B.role)
        assertEquals("垂直拉", slot2B.pattern)
        assertEquals(999L, slot2B.exerciseId)
        val slot1 = applied.first { it.slotKey == "1" }
        assertEquals("鳥狗式", slot1.name)
        assertEquals("核心平衡", slot1.role)
    }

    @Test
    fun dayBMissingExerciseKeepsTemplateRow() {
        val template = PlanFlex.slots.map { row(it, name = "A動作") }
        val applied = PlanFlex.templateFor(template, PlanFlexDay.B) { null }
        assertEquals(template, applied)
    }

    @Test
    fun memoryNamespacesDifferByDay() {
        assertNotEquals(
            PlanFlex.memoryNamespace(PlanFlexDay.A),
            PlanFlex.memoryNamespace(PlanFlexDay.B)
        )
        assertTrue(PlanFlex.memoryNamespace(PlanFlexDay.A).contains("A"))
    }

    @Test
    fun dayLabelsAndStorage() {
        assertEquals("A・後鏈＋胸", PlanFlex.dayLabel(PlanFlexDay.A))
        assertEquals("B・股四＋背闊＋肩", PlanFlex.dayLabel(PlanFlexDay.B))
        assertEquals(PlanFlexDay.B, PlanFlex.dayFromStored("B"))
        assertEquals(PlanFlexDay.A, PlanFlex.dayFromStored("A"))
        assertEquals(PlanFlexDay.A, PlanFlex.dayFromStored(null))
    }
}
