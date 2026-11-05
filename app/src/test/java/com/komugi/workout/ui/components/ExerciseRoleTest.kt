package com.komugi.workout.ui.components

import com.komugi.workout.data.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRoleTest {

    private fun exercise(
        name: String,
        parts: String,
        equipment: String = "啞鈴",
        movementType: String? = "WEIGHTED"
    ) = ExerciseEntity(
        id = 1,
        name = name,
        parts = parts,
        equipment = equipment,
        movementType = movementType
    )

    @Test
    fun lateralityIsInferredFromNameAndPattern() {
        assertEquals(
            ExerciseRole.LATERALITY_SINGLE,
            ExerciseRole.lateralityOf(exercise("單手壺鈴 RDL", "腿後腱、臀大肌", "8kg 壺鈴"))
        )
        assertEquals(
            ExerciseRole.LATERALITY_SINGLE,
            ExerciseRole.lateralityOf(exercise("保加利亞分腿蹲", "股四頭、臀大肌"))
        )
        assertEquals(
            ExerciseRole.LATERALITY_BILATERAL,
            ExerciseRole.lateralityOf(exercise("啞鈴 RDL", "腿後腱、臀大肌"))
        )
        assertEquals(
            ExerciseRole.LATERALITY_BILATERAL,
            ExerciseRole.lateralityOf(exercise("高腳杯深蹲", "股四頭、臀大肌", "壺鈴"))
        )
        assertEquals(
            ExerciseRole.LATERALITY_ANY,
            ExerciseRole.lateralityOf(exercise("平板支撐", "核心、肩、臀", "自重"))
        )
    }

    @Test
    fun movementPatternIsInferred() {
        assertEquals(
            ExerciseRole.PATTERN_PULL_VERTICAL,
            ExerciseRole.patternOf(exercise("滑輪下拉", "背闊肌、肱二頭", "滑輪"))
        )
        assertEquals(
            ExerciseRole.PATTERN_PULL_HORIZONTAL,
            ExerciseRole.patternOf(exercise("單手啞鈴划船", "背闊肌、菱形肌"))
        )
        assertEquals(
            ExerciseRole.PATTERN_PUSH_HORIZONTAL,
            ExerciseRole.patternOf(exercise("中性握地板臥推", "胸大肌、前三角、肱三頭"))
        )
        assertEquals(
            ExerciseRole.PATTERN_PUSH_VERTICAL,
            ExerciseRole.patternOf(exercise("阿諾德推舉", "三角肌、肱三頭"))
        )
        assertEquals(
            ExerciseRole.PATTERN_HINGE,
            ExerciseRole.patternOf(exercise("壺鈴擺盪", "臀大肌、腿後腱", "8kg 壺鈴"))
        )
        assertEquals(
            ExerciseRole.PATTERN_CORE,
            ExerciseRole.patternOf(exercise("平板支撐", "核心、肩、臀", "自重"))
        )
        assertEquals(
            ExerciseRole.PATTERN_HINGE,
            ExerciseRole.patternOf(
                exercise("單腳啞鈴 RDL", "腿後腱、臀大肌、核心（抗側屈）")
            )
        )
        assertEquals(
            ExerciseRole.PATTERN_SQUAT,
            ExerciseRole.patternOf(
                exercise("高腳杯深蹲", "股四頭、臀大肌、核心（前側負重）", "壺鈴")
            )
        )
    }

    @Test
    fun roleMatchingUsesPrimaryParts() {
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("單手壺鈴 RDL", "腿後腱、臀大肌", "8kg 壺鈴"),
                "腿後肌與臀肌"
            )
        )
        assertFalse(
            ExerciseRole.matchesRole(
                exercise("滑輪下拉", "背闊肌、肱二頭", "滑輪"),
                "上背肌"
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("滑輪臉拉", "三角肌後束、上背", "滑輪"),
                "上背肌"
            )
        )
        assertFalse(
            ExerciseRole.matchesRole(
                exercise("側平板支撐", "腹斜肌、臀中肌", "自重", "STATIC"),
                "腿後肌與臀肌"
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("單腳登階", "股四頭、臀大肌、腿後腱、小腿、核心", "啞鈴"),
                "腿後肌與臀肌"
            )
        )
    }

    @Test
    fun matchLabelDistinguishesFullAndPartialMatches() {
        val requirement = SlotRequirement(
            role = "腿後肌與臀肌",
            laterality = ExerciseRole.LATERALITY_SINGLE,
            pattern = ExerciseRole.PATTERN_HINGE
        )
        assertEquals(
            "完全符合",
            ExerciseRole.matchLabel(
                exercise("單手壺鈴 RDL", "腿後腱、臀大肌", "8kg 壺鈴"),
                requirement
            )
        )
        assertEquals(
            "部分符合（單雙邊不同）",
            ExerciseRole.matchLabel(
                exercise("啞鈴 RDL", "腿後腱、臀大肌"),
                requirement
            )
        )
    }

    @Test
    fun recommendationsAreAvailableForTemplateSlots() {
        val names = SlotRecommendations.namesFor("腿後肌與臀肌", "單邊", "髖主導")
        assertTrue(names.contains("單手壺鈴 RDL"))
        assertTrue(names.contains("單腳啞鈴 RDL"))

        val shoulder = SlotRecommendations.namesFor("肩膀", "通用", "垂直推")
        assertTrue(shoulder.contains("啞鈴肩推"))
        assertTrue(shoulder.contains("阿諾德推舉"))
    }
}
