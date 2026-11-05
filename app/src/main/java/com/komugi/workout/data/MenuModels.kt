package com.komugi.workout.data

import kotlinx.serialization.Serializable

@Serializable
data class MenuExercise(
    val name: String,
    val equipment: String? = null,
    val parts: String? = null,
    val sets: String? = null,
    val reps: String? = null,
    val rest: String? = null,
    val video: String? = null,
    val guide: String? = null,
    val superset: String? = null,
    val slot: String? = null,
    val role: String? = null,
    val laterality: String? = null,
    val pattern: String? = null
)

@Serializable
data class Menu(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val note: String? = null,
    val exercises: List<MenuExercise> = emptyList(),
    val template: Boolean = false
)

@Serializable
data class GuideRef(
    val id: String,
    val title: String,
    val asset: String,
    val subtitle: String? = null,
    val parts: String? = null
)

@Serializable
data class GuideIndex(
    val guides: List<GuideRef> = emptyList()
)

/**
 * 內建動作庫的補充動作（不屬於任何菜單，僅供動作庫與批次新增使用）。
 */
@Serializable
data class ExtraExercise(
    val name: String,
    val parts: String? = null,
    val equipment: String? = null,
    val location: String? = null,
    val movementType: String? = null,
    val difficulty: String? = null,
    val instructions: String? = null,
    val warning: String? = null
)

/**
 * 既有動作的補充說明（只更新動作主檔，不會新增動作）。
 */
@Serializable
data class ExerciseGuideSeed(
    val name: String,
    val instructions: String? = null,
    val warning: String? = null
)
