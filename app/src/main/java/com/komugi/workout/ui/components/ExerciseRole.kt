package com.komugi.workout.ui.components

import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.MenuExerciseRow

/**
 * 課表槽位的要求：角色、單雙邊、動作模式。
 * 菜單的 slot metadata 會轉成這個物件，用來篩選與排序替代動作。
 */
data class SlotRequirement(
    val role: String,
    val laterality: String? = null,
    val pattern: String? = null
)

object ExerciseRole {

    const val LATERALITY_SINGLE = "單邊"
    const val LATERALITY_BILATERAL = "雙邊"
    const val LATERALITY_ANY = "通用"

    const val PATTERN_CORE = "核心"
    const val PATTERN_HINGE = "髖主導"
    const val PATTERN_SQUAT = "蹲"
    const val PATTERN_PULL_HORIZONTAL = "水平拉"
    const val PATTERN_PULL_VERTICAL = "垂直拉"
    const val PATTERN_PUSH_HORIZONTAL = "水平推"
    const val PATTERN_PUSH_VERTICAL = "垂直推"
    const val PATTERN_RAISE = "平舉"
    const val PATTERN_OTHER = "其他"

    private val singleKeywords = listOf(
        "單腳", "單手", "單臂", "單邊", "分腿", "弓步", "登階", "跨步", "交替", "左右"
    )

    fun requirementOf(row: MenuExerciseRow, current: ExerciseEntity?): SlotRequirement {
        val role = row.role?.takeIf { it.isNotBlank() }
            ?: current?.let { roleOf(it) }
            ?: "其他"
        return SlotRequirement(
            role = role,
            laterality = row.laterality?.takeIf { it.isNotBlank() }
                ?: current?.let { lateralityOf(it) },
            pattern = row.pattern?.takeIf { it.isNotBlank() }
                ?: current?.let { patternOf(it) }
        )
    }

    fun roleOf(exercise: ExerciseEntity): String {
        val parts = exercise.parts.orEmpty()
        return when {
            parts.contains("胸大肌") -> "胸肌"
            parts.contains("背闊肌") -> "背闊肌"
            parts.contains("股四頭") -> "股四頭肌"
            parts.contains("腿後腱") || parts.contains("臀大肌") || parts.contains("臀中肌") ->
                "腿後肌與臀肌"
            parts.contains("三角肌") -> "肩膀"
            parts.contains("菱形肌") || parts.contains("斜方肌") -> "上背肌"
            parts.contains("腹") || parts.contains("核心") -> "核心"
            else -> "其他"
        }
    }

    fun lateralityOf(exercise: ExerciseEntity): String {
        if (singleKeywords.any { exercise.name.contains(it) }) return LATERALITY_SINGLE
        return when (patternOf(exercise)) {
            PATTERN_SQUAT, PATTERN_HINGE -> LATERALITY_BILATERAL
            else -> LATERALITY_ANY
        }
    }

    fun patternOf(exercise: ExerciseEntity): String {
        val name = exercise.name
        val parts = exercise.parts.orEmpty()
        return when {
            listOf(
                "核心", "腹直肌", "腹斜肌", "腹橫肌", "平板", "棒式",
                "死蟲", "鳥狗", "登山者", "捲腹", "舉腿"
            ).any { name.contains(it) } -> PATTERN_CORE

            listOf("硬舉", "RDL", "擺盪", "臀推", "早安").any { name.contains(it) } ->
                PATTERN_HINGE

            listOf("深蹲", "蹲", "腿推", "登階", "分腿", "弓步", "跨步").any { name.contains(it) } ->
                PATTERN_SQUAT

            name.contains("划船") -> PATTERN_PULL_HORIZONTAL
            listOf("下拉", "引體", "上拉").any { name.contains(it) } -> PATTERN_PULL_VERTICAL
            listOf("反向飛鳥", "側平舉", "臉拉", "聳肩").any { name.contains(it) } -> PATTERN_RAISE
            listOf("臥推", "伏地挺身", "飛鳥", "夾胸").any { name.contains(it) } ->
                PATTERN_PUSH_HORIZONTAL
            listOf("肩推", "推舉", "阿諾德").any { name.contains(it) } -> PATTERN_PUSH_VERTICAL

            // 名稱看不出模式時，用部位補強少數孤立動作。
            parts.contains("腿後腱") && name.contains("彎舉") -> PATTERN_HINGE
            parts.contains("股四頭") && name.contains("伸展") -> PATTERN_SQUAT
            else -> PATTERN_OTHER
        }
    }

    fun roleParts(role: String): List<String> = when {
        role.contains("核心") -> listOf("腹直肌", "腹斜肌", "腹橫肌", "核心", "豎脊肌", "髖屈肌")
        role.contains("腿後") || role.contains("臀") ->
            listOf("腿後腱", "臀大肌", "臀中肌", "臀小肌")
        role.contains("上背") ->
            listOf("菱形肌", "斜方肌", "中下斜方", "後三角", "三角肌後束", "旋轉肌群", "上背")
        role.contains("背闊") -> listOf("背闊肌")
        role.contains("股四頭") -> listOf("股四頭")
        role.contains("胸") -> listOf("胸大肌")
        role.contains("肩") -> listOf("三角肌")
        else -> listOf(role)
    }

    private fun roleGroups(role: String): Set<MuscleGroup> = when {
        role.contains("核心") -> setOf(MuscleGroup.CORE)
        role.contains("腿後") || role.contains("臀") ->
            setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS)
        role.contains("股四頭") -> setOf(MuscleGroup.QUADS)
        role.contains("上背") -> setOf(MuscleGroup.BACK, MuscleGroup.SHOULDERS)
        role.contains("背闊") -> setOf(MuscleGroup.BACK)
        role.contains("胸") -> setOf(MuscleGroup.CHEST)
        role.contains("肩") -> setOf(MuscleGroup.SHOULDERS)
        else -> emptySet()
    }

    fun matchesRole(exercise: ExerciseEntity, role: String?): Boolean {
        if (role.isNullOrBlank()) return true
        val parts = exercise.parts.orEmpty()
        if (parts.isBlank()) return false
        val groups = roleGroups(role)
        if (groups.isNotEmpty()) {
            val primary = primaryMuscleGroup(parts)
            if (primary != null && primary !in groups) return false
        }
        return roleParts(role).any { parts.contains(it) }
    }

    fun lateralityMatches(exercise: ExerciseEntity, wanted: String?): Boolean {
        if (wanted.isNullOrBlank() || wanted == LATERALITY_ANY) return true
        val actual = lateralityOf(exercise)
        return actual == wanted || actual == LATERALITY_ANY
    }

    fun isStrengthCandidate(exercise: ExerciseEntity): Boolean {
        if (exercise.name.contains("拉伸")) return false
        return ExerciseMeta.movementTypeOf(exercise) != ExerciseMeta.TYPE_CARDIO
    }

    fun matchScore(exercise: ExerciseEntity, requirement: SlotRequirement): Int {
        if (!matchesRole(exercise, requirement.role)) return -100
        var score = 20
        requirement.laterality?.takeIf { it != LATERALITY_ANY }?.let { wanted ->
            val actual = lateralityOf(exercise)
            score += when {
                actual == wanted -> 6
                actual == LATERALITY_ANY -> 1
                else -> -5
            }
        }
        requirement.pattern?.let { wanted ->
            val actual = patternOf(exercise)
            score += when {
                actual == wanted -> 4
                actual == PATTERN_OTHER -> 0
                else -> -2
            }
        }
        return score
    }

    fun matchLabel(exercise: ExerciseEntity, requirement: SlotRequirement): String {
        if (!matchesRole(exercise, requirement.role)) return "不符合"
        val lateralityOk = lateralityMatches(exercise, requirement.laterality)
        val patternOk = requirement.pattern == null ||
            patternOf(exercise) == requirement.pattern
        return when {
            lateralityOk && patternOk -> "完全符合"
            lateralityOk -> "部分符合（動作模式不同）"
            patternOk -> "部分符合（單雙邊不同）"
            else -> "部分符合"
        }
    }
}
