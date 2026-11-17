package com.komugi.workout.ui.components

import com.komugi.workout.data.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    private var nextId = 1L

    private fun exercise(
        name: String,
        parts: String?,
        equipment: String = "啞鈴"
    ) = ExerciseEntity(
        id = nextId++,
        name = name,
        parts = parts,
        equipment = equipment
    )

    private fun match(
        exercise: ExerciseEntity,
        keyword: String = "",
        category: MuscleGroup? = null,
        excludeCategory: Boolean = false,
        type: String? = null,
        location: String? = null,
        equipment: String? = null
    ) = ExerciseMeta.matchesLibraryFilter(
        exercise = exercise,
        keyword = keyword,
        category = category,
        excludeCategory = excludeCategory,
        type = type,
        location = location,
        equipment = equipment
    )

    @Test
    fun includeModeKeepsOnlyPrimaryCategory() {
        val bench = exercise("中性握地板臥推", "胸大肌、前三角、肱三頭")
        val row = exercise("單手啞鈴划船", "背闊肌、菱形肌、中下斜方、肱二頭（輔助）")

        assertTrue(match(bench, category = MuscleGroup.CHEST))
        assertFalse(match(row, category = MuscleGroup.CHEST))
        // 臥推的主要部位是胸，包含模式選三頭時不會出現
        assertFalse(match(bench, category = MuscleGroup.TRICEPS))
    }

    @Test
    fun excludeModeHidesAnyMentionedGroup() {
        val bench = exercise("中性握地板臥推", "胸大肌、前三角、肱三頭")
        val overhead = exercise("啞鈴過頭三頭伸展", "肱三頭、肩")
        val row = exercise("單手啞鈴划船", "背闊肌、菱形肌、中下斜方、肱二頭（輔助）")

        assertFalse(match(bench, category = MuscleGroup.TRICEPS, excludeCategory = true))
        assertFalse(match(overhead, category = MuscleGroup.TRICEPS, excludeCategory = true))
        assertTrue(match(row, category = MuscleGroup.TRICEPS, excludeCategory = true))
    }

    @Test
    fun excludeModeWithoutCategoryChangesNothing() {
        val bench = exercise("中性握地板臥推", "胸大肌、前三角、肱三頭")

        assertTrue(match(bench, excludeCategory = true))
    }

    @Test
    fun otherFiltersStillApplyInExcludeMode() {
        val bench = exercise("中性握地板臥推", "胸大肌、前三角、肱三頭", equipment = "啞鈴")
        val row = exercise("單手啞鈴划船", "背闊肌、菱形肌、中下斜方、肱二頭（輔助）", equipment = "壺鈴")

        assertFalse(match(bench, category = MuscleGroup.TRICEPS, excludeCategory = true))
        assertTrue(
            match(
                row,
                category = MuscleGroup.TRICEPS,
                excludeCategory = true,
                equipment = "壺鈴"
            )
        )
        assertFalse(
            match(
                row,
                category = MuscleGroup.TRICEPS,
                excludeCategory = true,
                equipment = "啞鈴"
            )
        )
        assertFalse(match(row, category = MuscleGroup.TRICEPS, excludeCategory = true, keyword = "臥推"))
    }

    @Test
    fun multiEquipmentMatchesAnyCategory() {
        val fly = exercise("反向飛鳥", "後三角、菱形肌、中下斜方", equipment = "啞鈴、彈力帶")
        assertEquals(listOf("啞鈴", "彈力帶"), ExerciseMeta.equipmentCategoryList("啞鈴、彈力帶"))
        assertEquals("啞鈴", ExerciseMeta.equipmentCategory("啞鈴、彈力帶"))
        assertTrue(match(fly, equipment = "啞鈴"))
        assertTrue(match(fly, equipment = "彈力帶"))
        assertFalse(match(fly, equipment = "槓鈴"))

        val combo = exercise("TRX 複合", "核心", equipment = "TRX＋啞鈴")
        assertTrue(match(combo, equipment = "TRX"))
        assertTrue(match(combo, equipment = "啞鈴"))

        val cable = exercise("纜繩動作", "核心", equipment = "纜繩或藥球")
        assertTrue(match(cable, equipment = "滑輪"))
    }

    @Test
    fun cableEquipmentMapsToPulleyCategory() {
        assertEquals("滑輪", ExerciseMeta.equipmentCategory("滑輪、龍門架"))
        assertEquals("滑輪", ExerciseMeta.equipmentCategory("滑輪、繩索"))
        assertEquals("滑輪", ExerciseMeta.equipmentCategory("纜繩"))
        assertEquals("滑輪", ExerciseMeta.equipmentCategory("纜繩或藥球"))
        // 既有分類不受影響
        assertEquals("啞鈴", ExerciseMeta.equipmentCategory("啞鈴"))
        assertEquals("彈力帶", ExerciseMeta.equipmentCategory("30kg 帶"))
        assertTrue(ExerciseMeta.equipmentCategories.contains("滑輪"))
    }
}
