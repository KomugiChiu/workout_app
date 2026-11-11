package com.komugi.workout.ui.components

import com.komugi.workout.data.ExerciseEntity
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
}
