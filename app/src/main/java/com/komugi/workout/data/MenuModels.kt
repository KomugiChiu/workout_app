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
    val guide: String? = null
)

@Serializable
data class Menu(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val note: String? = null,
    val exercises: List<MenuExercise> = emptyList()
)

@Serializable
data class GuideRef(
    val id: String,
    val title: String,
    val asset: String,
    val subtitle: String? = null
)

@Serializable
data class GuideIndex(
    val guides: List<GuideRef> = emptyList()
)
