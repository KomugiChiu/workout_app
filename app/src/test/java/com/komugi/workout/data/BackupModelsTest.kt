package com.komugi.workout.data

import com.komugi.workout.util.TargetParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupModelsTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun backupRoundTripKeepsAllFields() {
        val backup = BackupFile(
            version = BackupFile.BACKUP_VERSION,
            exportedAt = 1_700_000_000_000L,
            exercises = listOf(
                BackupExercise(
                    id = 7,
                    name = "上斜啞鈴臥推",
                    parts = "胸大肌、前三角、肱三頭",
                    equipment = "啞鈴、斜椅",
                    location = "HOME",
                    movementType = "WEIGHTED",
                    difficulty = "INTERMEDIATE",
                    instructions = "步驟一\n步驟二\n步驟三",
                    warning = "注意肩部",
                    isCustom = true
                )
            ),
            menus = listOf(
                BackupMenu(
                    id = "custom-test",
                    name = "測試菜單",
                    subtitle = "副標",
                    orderIndex = 9,
                    isCustom = true
                )
            ),
            menuExercises = listOf(
                BackupMenuExercise(
                    id = 3,
                    menuId = "custom-test",
                    exerciseId = 7,
                    orderIndex = 0,
                    equipment = "啞鈴",
                    targetSets = "4",
                    targetReps = "8–10",
                    restSec = "90",
                    supersetKey = "A"
                )
            ),
            sessions = listOf(
                BackupSession(
                    id = 11,
                    dateEpochDay = 20_760,
                    menuId = "custom-test",
                    menuName = "測試菜單",
                    note = "備註",
                    startedAt = 1_700_000_000_000L,
                    finishedAt = 1_700_000_900_000L
                )
            ),
            entries = listOf(
                BackupEntry(
                    id = 21,
                    sessionId = 11,
                    name = "上斜啞鈴臥推",
                    equipment = "啞鈴",
                    targetParts = "胸大肌",
                    exerciseId = 7,
                    orderIndex = 0,
                    targetSets = "4",
                    targetReps = "8–10",
                    restSec = "90",
                    supersetKey = "A"
                )
            ),
            sets = listOf(
                BackupSet(
                    id = 31,
                    entryId = 21,
                    setIndex = 1,
                    weightKg = 20.0,
                    reps = 8,
                    durationSec = 45,
                    isBodyweight = false
                )
            )
        )

        val text = json.encodeToString(backup)
        val decoded = json.decodeFromString<BackupFile>(text)

        assertEquals(backup, decoded)
        assertEquals(BackupFile.BACKUP_VERSION, decoded.version)
    }

    @Test
    fun restSecondsParsesCommonMenuFormats() {
        assertEquals(90, TargetParser.restSeconds("90s"))
        assertEquals(60, TargetParser.restSeconds("60–90s"))
        assertEquals(60, TargetParser.restSeconds("休 60"))
        assertEquals(120, TargetParser.restSeconds("120 秒"))
        assertEquals(10, TargetParser.restSeconds("5s"))
        assertEquals(600, TargetParser.restSeconds("9999s"))
        assertNull(TargetParser.restSeconds(null))
        assertNull(TargetParser.restSeconds("無"))
    }

    @Test
    fun oldBackupWithoutNewFieldsStillDecodes() {
        val text = """
            {
              "version": 1,
              "exportedAt": 123,
              "sessions": [
                { "id": 1, "dateEpochDay": 20760 }
              ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString<BackupFile>(text)

        assertEquals(1, decoded.sessions.size)
        assertEquals(20760L, decoded.sessions.first().dateEpochDay)
        assertNull(decoded.sessions.first().startedAt)
        assertNull(decoded.sessions.first().finishedAt)
    }

    @Test
    fun emptyBackupIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupFile().requireImportable()
        }
    }

    @Test
    fun newerBackupVersionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupFile(
                version = 99,
                exercises = listOf(
                    BackupExercise(id = 1, name = "測試")
                )
            ).requireImportable()
        }
    }
}
