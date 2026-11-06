package com.komugi.workout.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleMapTest {

    @Test
    fun fullBodyMatchesExplicitTag() {
        assertTrue(isFullBodyExercise("全身（股四頭、胸、三頭、核心）＋心肺"))
    }

    @Test
    fun fullBodyRequiresUpperLowerAndCore() {
        // 上肢＋下肢＋核心：算全身
        assertTrue(isFullBodyExercise("股四頭、臀大肌、三角肌、肱三頭、核心"))
        assertTrue(isFullBodyExercise("臀大肌、腿後腱、豎脊肌、握力、核心"))
        // 缺核心：不算
        assertFalse(isFullBodyExercise("腿後腱、臀大肌、豎脊肌"))
        // 缺下肢：不算
        assertFalse(isFullBodyExercise("胸大肌、前三角、肱三頭、核心"))
        // 缺上肢：不算
        assertFalse(isFullBodyExercise("股四頭、臀大肌、腿後腱、核心"))
        // 空值：不算
        assertFalse(isFullBodyExercise(null))
        assertFalse(isFullBodyExercise(""))
    }

    @Test
    fun deadliftFamilyCountsAsFullBodyAfterCoreTag() {
        assertTrue(isFullBodyExercise("臀大肌、腿後腱、豎脊肌、握力、核心"))
        assertTrue(isFullBodyExercise("三角肌、斜方肌、臀大肌、腿後腱、核心"))
    }
}
