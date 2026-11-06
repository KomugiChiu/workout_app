package com.komugi.workout.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuSeedTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun planTemplatesAreWellFormed() {
        val menus = json.decodeFromString<List<Menu>>(
            File("src/main/assets/menus.json").readText()
        )
        assertEquals(12, menus.size)

        val planA = menus.first { it.id == "plan-a" }
        val planB = menus.first { it.id == "plan-b" }
        assertTrue(planA.template)
        assertTrue(planB.template)

        val expectedSlots = listOf("1", "2A", "2B", "3A", "3B")
        val expectedSupersets = listOf(null, "2", "2", "3", "3")
        listOf(planA, planB).forEach { plan ->
            assertEquals(5, plan.exercises.size)
            assertEquals(expectedSlots, plan.exercises.map { it.slot })
            assertEquals(expectedSupersets, plan.exercises.map { it.superset })
            plan.exercises.forEach { exercise ->
                assertEquals("3", exercise.sets)
                assertEquals("12", exercise.reps)
                assertEquals("60", exercise.rest)
                assertNotNull(exercise.role)
                assertNotNull(exercise.laterality)
                assertNotNull(exercise.pattern)
            }
        }
    }

    @Test
    fun threeExerciseTemplateIsWellFormed() {
        val menus = json.decodeFromString<List<Menu>>(
            File("src/main/assets/menus.json").readText()
        )
        val three = menus.first { it.id == "three-exercise" }
        assertTrue(three.template)
        assertEquals(3, three.exercises.size)
        assertEquals(listOf("1", "2", "3"), three.exercises.map { it.slot })
        assertEquals(
            listOf("下肢", "上肢拉", "上肢推"),
            three.exercises.map { it.role }
        )
        assertTrue(three.exercises.all { it.superset == null })
        assertTrue(three.exercises.all { it.sets == "4-5" })
        assertTrue(three.exercises.all { it.reps == "6-8" })
        assertEquals(
            listOf("75", "75", "60"),
            three.exercises.map { it.rest }
        )
    }

    @Test
    fun extraSeedContainsReplacementExercises() {
        val extras = json.decodeFromString<List<ExtraExercise>>(
            File("src/main/assets/exercises_extra.json").readText()
        )
        val names = extras.map { it.name }.toSet()
        assertTrue(
            names.containsAll(
                listOf("單腳啞鈴 RDL", "啞鈴深蹲", "直臂下拉", "啞鈴肩推")
            )
        )
    }
}
