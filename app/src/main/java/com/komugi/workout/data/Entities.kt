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
    val note: String? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null
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
    val exerciseId: Long? = null,
    val orderIndex: Int = 0,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val videoUrl: String? = null,
    val guideAsset: String? = null,
    val supersetKey: String? = null
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

/** 單一動作的歷史組數（跨訓練紀錄），用於動作進度頁。 */
data class ExerciseSetRow(
    val sessionId: Long,
    val dateEpochDay: Long,
    val menuName: String?,
    val entryId: Long,
    val entryName: String,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val isBodyweight: Boolean
)

@Entity(
    tableName = "body_metrics",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 動作主檔：所有動作的唯一來源（菜單動作＋自訂動作）。
 */
@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parts: String? = null,
    val videoUrl: String? = null,
    val guideId: String? = null,
    val isCustom: Boolean = false,
    val createdAt: Long = 0,
    val equipment: String? = null,
    val location: String? = null,
    val movementType: String? = null,
    val difficulty: String? = null,
    val instructions: String? = null,
    val warning: String? = null
)

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String? = null,
    val note: String? = null,
    val orderIndex: Int = 0,
    val isCustom: Boolean = false
)

/**
 * 菜單與動作的關聯（多對多）：同一動作可出現在多份菜單，
 * 每份菜單可各自指定器材、組數、次數與休息。
 */
@Entity(
    tableName = "menu_exercises",
    foreignKeys = [
        ForeignKey(
            entity = MenuEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("menuId"), Index("exerciseId")]
)
data class MenuExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val menuId: String,
    val exerciseId: Long,
    val orderIndex: Int,
    val equipment: String? = null,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val supersetKey: String? = null
)

data class MenuExerciseRow(
    val menuExerciseId: Long,
    val menuId: String,
    val exerciseId: Long,
    val orderIndex: Int,
    val equipment: String?,
    val targetSets: String?,
    val targetReps: String?,
    val restSec: String?,
    val name: String,
    val parts: String?,
    val videoUrl: String?,
    val guideId: String?,
    val supersetKey: String?
)

data class MenuSummary(
    @Embedded val menu: MenuEntity,
    val exerciseCount: Int
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)
