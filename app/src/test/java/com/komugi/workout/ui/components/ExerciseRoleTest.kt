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
    fun genericRolesMatchWholeRegions() {
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("高腳杯深蹲", "股四頭、臀大肌、核心（前側負重）", "壺鈴"),
                ExerciseRole.ROLE_LOWER
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("單手壺鈴 RDL", "腿後腱、臀大肌", "8kg 壺鈴"),
                ExerciseRole.ROLE_LOWER
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("引體向上", "背闊肌、肱二頭、核心", "單槓"),
                ExerciseRole.ROLE_PULL
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("伏地挺身", "胸大肌、前三角、肱三頭、核心", "自重"),
                ExerciseRole.ROLE_PUSH
            )
        )
        assertFalse(
            ExerciseRole.matchesRole(
                exercise("滑輪下拉", "背闊肌、肱二頭", "滑輪"),
                ExerciseRole.ROLE_PUSH
            )
        )
        assertFalse(
            ExerciseRole.matchesRole(
                exercise("平板支撐", "核心、肩、臀", "自重"),
                ExerciseRole.ROLE_LOWER
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

        val lower = SlotRecommendations.namesFor("下肢", "通用", null)
        assertTrue(lower.contains("高腳杯深蹲"))
        assertTrue(lower.contains("啞鈴 RDL"))

        val pull = SlotRecommendations.namesFor("上肢拉", "通用", null)
        assertTrue(pull.contains("單手啞鈴划船"))
        assertTrue(pull.contains("引體向上"))

        val push = SlotRecommendations.namesFor("上肢推", "通用", null)
        assertTrue(push.contains("中性握地板臥推"))
        assertTrue(push.contains("啞鈴肩推"))
    }

    @Test
    fun compoundSlotAcceptsLegDrivenFullBodyMoves() {
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("胸前蹲推 Thruster", "股四頭、臀大肌、三角肌、肱三頭、核心"),
                ExerciseRole.ROLE_COMPOUND
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("啞鈴借力推舉", "三角肌、肱三頭、股四頭、核心"),
                ExerciseRole.ROLE_COMPOUND
            )
        )
        assertFalse(
            ExerciseRole.matchesRole(
                exercise("側平舉", "三角肌中束"),
                ExerciseRole.ROLE_COMPOUND
            )
        )
        val names = SlotRecommendations.namesFor(
            ExerciseRole.ROLE_COMPOUND, ExerciseRole.LATERALITY_BILATERAL, ExerciseRole.PATTERN_SQUAT
        )
        assertTrue(names.contains("胸前蹲推 Thruster"))
        assertTrue(names.contains("啞鈴借力推舉"))
    }

    @Test
    fun cardioSlotAcceptsHeartRateMoves() {
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("平板開合跳", "核心、肩胛穩定、髖屈肌、心肺", "自重", null),
                ExerciseRole.ROLE_CARDIO
            )
        )
        assertTrue(
            ExerciseRole.matchesRole(
                exercise("划船機", "背闊肌、腿後腱、心肺", "划船機", "CARDIO"),
                ExerciseRole.ROLE_CARDIO
            )
        )
        assertTrue(
            ExerciseRole.isCardioCandidate(
                exercise("平板開合跳", "核心、肩胛穩定、髖屈肌、心肺", "自重", null)
            )
        )
        assertFalse(
            ExerciseRole.isCardioCandidate(
                exercise("六角槓硬舉", "臀大肌、腿後腱、股四頭、豎脊肌、握力、核心")
            )
        )
        val names = SlotRecommendations.namesFor(
            ExerciseRole.ROLE_CARDIO, ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_CORE
        )
        assertTrue(names.contains("平板開合跳"))
        assertTrue(names.contains("無跳波比"))
        assertTrue(names.contains("划船機"))
    }
}
