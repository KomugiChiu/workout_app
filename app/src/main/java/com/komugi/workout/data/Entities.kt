package com.komugi.workout.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val menuId: String? = null,
    val menuName: String? = null,
    val note: String? = null
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val equipment: String? = null,
    val targetParts: String? = null,
    val orderIndex: Int = 0,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val videoUrl: String? = null,
    val guideAsset: String? = null
)

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val setIndex: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val isBodyweight: Boolean = false
)

data class SessionSummary(
    @Embedded val session: SessionEntity,
    val entryCount: Int
)

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey val name: String,
    val addedAt: Long
)
