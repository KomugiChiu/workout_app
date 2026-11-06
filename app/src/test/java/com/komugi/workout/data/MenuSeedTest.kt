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
        assertEquals(88, menus.size)

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
    fun cycleFlexTemplateIsWellFormed() {
        val menus = json.decodeFromString<List<Menu>>(
            File("src/main/assets/menus.json").readText()
        )
        val cycle = menus.first { it.id == "cycle-flex" }
        assertTrue(cycle.template)
        assertEquals(
            listOf("H", "T1", "T2", "T3", "T4", "T5", "F"),
            cycle.exercises.map { it.slot }
        )
        // 無配對組：頭尾＋主幹依序執行
        assertTrue(cycle.exercises.all { it.superset == null })
        // T4 種子值為 A 天預設；B 天 2–3 組由 CycleFlex.applyDay 動態套用
        assertEquals("3", cycle.exercises.first { it.slot == "T4" }.sets)
        cycle.exercises.forEach { exercise ->
            assertNotNull(exercise.role)
            assertNotNull(exercise.laterality)
        }
        // 心肺收尾槽走有氧候選池
        assertEquals("心肺", cycle.exercises.first { it.slot == "F" }.role)
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
    fun bookBatch1MenusAreWellFormed() {
        val menus = json.decodeFromString<List<Menu>>(
            File("src/main/assets/menus.json").readText()
        )
        val crowdedA = menus.first { it.id == "crowded-a" }
        assertEquals(4, crowdedA.exercises.size)
        assertEquals(
            listOf("啞鈴臥推", "跪姿支撐啞鈴划船", "上斜啞鈴臥推", "啞鈴深蹲"),
            crowdedA.exercises.map { it.name }
        )
        assertEquals(
            listOf("1", "1", "2", "2"),
            crowdedA.exercises.map { it.superset }
        )

        val crowdedB = menus.first { it.id == "crowded-b" }
        assertEquals(4, crowdedB.exercises.size)
        assertEquals("啞鈴直腿硬舉", crowdedB.exercises[2].name)
        assertEquals("0", crowdedB.exercises[2].rest)

        val crowdedC = menus.first { it.id == "crowded-c" }
        assertEquals(4, crowdedC.exercises.size)

        val bw1 = menus.first { it.id == "bw-1" }
        assertEquals(5, bw1.exercises.size)
        val bw2 = menus.first { it.id == "bw-2" }
        assertEquals(5, bw2.exercises.size)
        val bw3 = menus.first { it.id == "bw-3" }
        assertEquals(8, bw3.exercises.size)
        assertTrue(bw3.exercises.all { it.superset == "1" })

        listOf(crowdedA, crowdedB, crowdedC, bw1, bw2, bw3).forEach { menu ->
            assertTrue(!menu.template)
            menu.exercises.forEach { exercise ->
                assertTrue(exercise.name.isNotBlank())
                assertTrue(exercise.sets!!.isNotBlank())
                assertTrue(exercise.reps!!.isNotBlank())
            }
        }
    }

    @Test
    fun bookBatch2MenusAreWellFormed() {
        val menus = json.decodeFromString<List<Menu>>(
            File("src/main/assets/menus.json").readText()
        )
        val expected = mapOf(
            "getback-p1" to 6, "getback-p2" to 6,
            "getback-p3a" to 6, "getback-p3b" to 6,
            "crowded2-a" to 3, "crowded2-b" to 3, "crowded2-c" to 3,
            "crowded3-a" to 5, "crowded3-b" to 5,
            "fatloss-p1a" to 6, "fatloss-p1b" to 6, "fatloss-p1c" to 6,
            "fatloss-p2a" to 5, "fatloss-p2b" to 5, "fatloss-p2c" to 5,
            "fatloss-p3a" to 6, "fatloss-p3b" to 5, "fatloss-p3c" to 6,
            "beach-p1a" to 5, "beach-p1b" to 5, "beach-p1c" to 5,
            "beach-p2a" to 5, "beach-p2b" to 4, "beach-p2c" to 5, "beach-p2d" to 4,
            "beach-p3a" to 5, "beach-p3b" to 5, "beach-p3c" to 5,
            "beach-p4a" to 5, "beach-p4b" to 4, "beach-p4c" to 5, "beach-p4d" to 4,
            "wedding-p1a" to 6, "wedding-p1b" to 6, "wedding-p1c" to 5,
            "wedding-p2a" to 6, "wedding-p2b" to 6, "wedding-p2c" to 5,
            "vertical-p1a" to 6, "vertical-p1b" to 6,
            "vertical-p2a" to 5, "vertical-p2b" to 6,
            "scrawny-a" to 10, "scrawny-b" to 5, "scrawny-c" to 10,
            "sports-a" to 9, "sports-b" to 8, "sports-c" to 9, "sports-d" to 9,
            "bigbench-a" to 6, "bigbench-b" to 5,
            "couples-a" to 4, "couples-b" to 5, "couples-c" to 5,
            "couples-f1" to 4, "couples-f2" to 4, "couples-f3" to 4,
            "couples-f4" to 4, "couples-f5" to 4, "couples-f6" to 4,
            "min15-01" to 5, "min15-02" to 5, "min15-03" to 4, "min15-04" to 4,
            "min15-05" to 4, "min15-06" to 5, "min15-07" to 4, "min15-08" to 5,
            "min15-09" to 4, "min15-10" to 5,
            "spartacus" to 10
        )
        assertEquals(71, expected.size)
        expected.forEach { (id, count) ->
            val menu = menus.first { it.id == id }
            assertEquals(id, count, menu.exercises.size)
            assertTrue(id, !menu.template)
            menu.exercises.forEach { exercise ->
                assertTrue(id, exercise.name.isNotBlank())
                assertTrue(id, exercise.sets!!.isNotBlank())
                assertTrue(id, exercise.reps!!.isNotBlank())
                assertTrue(id, exercise.rest!!.isNotBlank())
            }
        }
        // 招牌動作抽查
        assertEquals(
            "槓鈴硬舉",
            menus.first { it.id == "vertical-p1a" }.exercises[3].name
        )
        assertEquals(
            "深蹲推撐",
            menus.first { it.id == "wedding-p1c" }.exercises[4].name
        )
    }

    @Test
    fun referenceMenusAreMarkedForDataTab() {
        val menus = json.decodeFromString<List<Menu>>(
            File("src/main/assets/menus.json").readText()
        )
        val reference = menus.filter { it.reference }
        val trainable = menus.filterNot { it.reference }
        assertEquals(77, reference.size)
        assertEquals(11, trainable.size)
        // 可調模板留在菜單頁（資料頁只做入口）
        assertTrue(menus.first { it.id == "plan-a" }.template)
        assertTrue(!menus.first { it.id == "plan-a" }.reference)
        assertTrue(menus.first { it.id == "three-exercise" }.template)
        assertTrue(menus.first { it.id == "cycle-flex" }.template)
        assertTrue(!menus.first { it.id == "cycle-flex" }.reference)
        assertEquals("肌力團課", menus.first { it.id == "cycle-flex" }.name)
        assertEquals("肌力團課2", menus.first { it.id == "cycle-c" }.name)
        assertTrue(menus.none { it.id == "cycle-a" || it.id == "cycle-b" })
        // 書本課表標記參考用
        assertTrue(menus.first { it.id == "crowded-a" }.reference)
        assertTrue(menus.first { it.id == "spartacus" }.reference)
        // App 原創留在菜單頁
        assertTrue(!menus.first { it.id == "home-light" }.reference)
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
