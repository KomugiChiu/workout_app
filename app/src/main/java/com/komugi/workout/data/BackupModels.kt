package com.komugi.workout.data

import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val version: Int = BACKUP_VERSION,
    val exportedAt: Long = 0L,
    val exercises: List<BackupExercise> = emptyList(),
    val menus: List<BackupMenu> = emptyList(),
    val menuExercises: List<BackupMenuExercise> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
    val entries: List<BackupEntry> = emptyList(),
    val sets: List<BackupSet> = emptyList()
) {
    companion object {
        const val BACKUP_VERSION = 1
    }
}

@Serializable
data class BackupExercise(
    val id: Long,
    val name: String,
    val parts: String? = null,
    val videoUrl: String? = null,
    val guideId: String? = null,
    val isCustom: Boolean = false,
    val createdAt: Long = 0L,
    val equipment: String? = null,
    val location: String? = null,
    val movementType: String? = null,
    val difficulty: String? = null,
    val instructions: String? = null,
    val warning: String? = null
)

@Serializable
data class BackupMenu(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val note: String? = null,
    val orderIndex: Int = 0,
    val isCustom: Boolean = false
)

@Serializable
data class BackupMenuExercise(
    val id: Long = 0L,
    val menuId: String,
    val exerciseId: Long,
    val orderIndex: Int = 0,
    val equipment: String? = null,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val supersetKey: String? = null
)

@Serializable
data class BackupSession(
    val id: Long,
    val dateEpochDay: Long,
    val menuId: String? = null,
    val menuName: String? = null,
    val note: String? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null
)

@Serializable
data class BackupEntry(
    val id: Long,
    val sessionId: Long,
    val name: String,
    val equipment: String? = null,
    val targetParts: String? = null,
    val exerciseId: Long? = null,
    val orderIndex: Int = 0,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val videoUrl: String? = null,
    val guideAsset: String? = null,
    val supersetKey: String? = null
)

@Serializable
data class BackupSet(
    val id: Long,
    val entryId: Long,
    val setIndex: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val isBodyweight: Boolean = false
)

fun ExerciseEntity.toBackup(): BackupExercise = BackupExercise(
    id = id,
    name = name,
    parts = parts,
    videoUrl = videoUrl,
    guideId = guideId,
    isCustom = isCustom,
    createdAt = createdAt,
    equipment = equipment,
    location = location,
    movementType = movementType,
    difficulty = difficulty,
    instructions = instructions,
    warning = warning
)

fun BackupExercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    parts = parts,
    videoUrl = videoUrl,
    guideId = guideId,
    isCustom = isCustom,
    createdAt = createdAt,
    equipment = equipment,
    location = location,
    movementType = movementType,
    difficulty = difficulty,
    instructions = instructions,
    warning = warning
)

fun MenuEntity.toBackup(): BackupMenu = BackupMenu(
    id = id,
    name = name,
    subtitle = subtitle,
    note = note,
    orderIndex = orderIndex,
    isCustom = isCustom
)

fun BackupMenu.toEntity(): MenuEntity = MenuEntity(
    id = id,
    name = name,
    subtitle = subtitle,
    note = note,
    orderIndex = orderIndex,
    isCustom = isCustom
)

fun MenuExerciseEntity.toBackup(): BackupMenuExercise = BackupMenuExercise(
    id = id,
    menuId = menuId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    equipment = equipment,
    targetSets = targetSets,
    targetReps = targetReps,
    restSec = restSec,
    supersetKey = supersetKey
)

fun BackupMenuExercise.toEntity(): MenuExerciseEntity = MenuExerciseEntity(
    id = id,
    menuId = menuId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    equipment = equipment,
    targetSets = targetSets,
    targetReps = targetReps,
    restSec = restSec,
    supersetKey = supersetKey
)

fun SessionEntity.toBackup(): BackupSession = BackupSession(
    id = id,
    dateEpochDay = dateEpochDay,
    menuId = menuId,
    menuName = menuName,
    note = note,
    startedAt = startedAt,
    finishedAt = finishedAt
)

fun BackupSession.toEntity(): SessionEntity = SessionEntity(
    id = id,
    dateEpochDay = dateEpochDay,
    menuId = menuId,
    menuName = menuName,
    note = note,
    startedAt = startedAt,
    finishedAt = finishedAt
)

fun EntryEntity.toBackup(): BackupEntry = BackupEntry(
    id = id,
    sessionId = sessionId,
    name = name,
    equipment = equipment,
    targetParts = targetParts,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    targetSets = targetSets,
    targetReps = targetReps,
    restSec = restSec,
    videoUrl = videoUrl,
    guideAsset = guideAsset,
    supersetKey = supersetKey
)

fun BackupEntry.toEntity(): EntryEntity = EntryEntity(
    id = id,
    sessionId = sessionId,
    name = name,
    equipment = equipment,
    targetParts = targetParts,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    targetSets = targetSets,
    targetReps = targetReps,
    restSec = restSec,
    videoUrl = videoUrl,
    guideAsset = guideAsset,
    supersetKey = supersetKey
)

fun SetEntity.toBackup(): BackupSet = BackupSet(
    id = id,
    entryId = entryId,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    durationSec = durationSec,
    isBodyweight = isBodyweight
)

fun BackupSet.toEntity(): SetEntity = SetEntity(
    id = id,
    entryId = entryId,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    durationSec = durationSec,
    isBodyweight = isBodyweight
)
