package com.komugi.workout.ui.components

import com.komugi.workout.data.ExerciseEntity

/**
 * 動作庫的分類與標籤規則。
 *
 * 分類（category）只取主要訓練部位；器材、地點、型態、難度是獨立標籤，
 * 避免把不同維度塞進同一個分類。
 */
object ExerciseMeta {

    const val LOCATION_ANY = "ANY"
    const val LOCATION_HOME = "HOME"
    const val LOCATION_GYM = "GYM"
    const val LOCATION_OUTDOOR = "OUTDOOR"

    const val TYPE_WEIGHTED = "WEIGHTED"
    const val TYPE_BODYWEIGHT = "BODYWEIGHT"
    const val TYPE_STATIC = "STATIC"
    const val TYPE_CARDIO = "CARDIO"

    const val DIFFICULTY_BEGINNER = "BEGINNER"
    const val DIFFICULTY_INTERMEDIATE = "INTERMEDIATE"
    const val DIFFICULTY_ADVANCED = "ADVANCED"

    val equipmentCategories = listOf(
        "自重", "啞鈴", "壺鈴", "槓鈴", "槓片", "彈力帶", "TRX", "有氧", "其他"
    )

    val locations = listOf(
        LOCATION_ANY, LOCATION_HOME, LOCATION_GYM, LOCATION_OUTDOOR
    )

    val movementTypes = listOf(
        TYPE_WEIGHTED, TYPE_BODYWEIGHT, TYPE_STATIC, TYPE_CARDIO
    )

    val difficulties = listOf(
        DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_ADVANCED
    )

    fun locationLabel(value: String?): String = when (value) {
        LOCATION_HOME -> "居家"
        LOCATION_GYM -> "健身房"
        LOCATION_OUTDOOR -> "戶外"
        else -> "通用"
    }

    fun movementTypeLabel(value: String?): String = when (value) {
        TYPE_BODYWEIGHT -> "徒手"
        TYPE_STATIC -> "靜態"
        TYPE_CARDIO -> "有氧"
        else -> "負重"
    }

    fun difficultyLabel(value: String?): String = when (value) {
        DIFFICULTY_BEGINNER -> "初階"
        DIFFICULTY_ADVANCED -> "進階"
        else -> "中階"
    }

    fun equipmentCategory(raw: String?): String {
        val text = raw?.trim().orEmpty()
        return when {
            text.isEmpty() -> "其他"
            text.contains("自重") -> "自重"
            text.contains("壺鈴") || text.contains("ViPR", ignoreCase = true) -> "壺鈴"
            text.contains("TRX") || text.contains("懸吊") -> "TRX"
            text.contains("彈力帶") || text.contains("帶") -> "彈力帶"
            text.contains("槓片") -> "槓片"
            text.contains("槓") -> "槓鈴"
            text.contains("啞鈴") -> "啞鈴"
            Regex("\\d+\\s*kg", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "啞鈴"
            text.contains("跑步") || text.contains("腳踏車") || text.contains("划船機") ||
                text.contains("橢圓") || text.contains("踏步") || text.contains("跳繩") -> "有氧"
            else -> "其他"
        }
    }

    fun categoryOf(exercise: ExerciseEntity): MuscleGroup? =
        primaryMuscleGroup(exercise.parts)

    /**
     * 動作庫篩選條件。分類有包含／排除兩種模式：
     * 包含只看主要部位；排除則看部位文字提到的所有部位
     *（例如隱藏三頭時，部位含肱三頭的臥推也會被隱藏）。
     */
    fun matchesLibraryFilter(
        exercise: ExerciseEntity,
        keyword: String,
        category: MuscleGroup?,
        excludeCategory: Boolean,
        type: String?,
        location: String?,
        equipment: String?
    ): Boolean {
        if (keyword.isNotBlank() && !exercise.name.contains(keyword, ignoreCase = true)) {
            return false
        }
        if (category != null) {
            if (excludeCategory) {
                if (muscleGroupsIn(exercise.parts).contains(category)) return false
            } else if (categoryOf(exercise) != category) {
                return false
            }
        }
        if (type != null && movementTypeOf(exercise) != type) return false
        if (location != null && locationOf(exercise) != location) return false
        if (equipment != null && equipmentCategory(exercise.equipment) != equipment) return false
        return true
    }

    fun categoryLabel(exercise: ExerciseEntity): String =
        categoryOf(exercise)?.let { muscleGroupLabel(it) } ?: "未分類"

    fun locationOf(exercise: ExerciseEntity): String =
        exercise.location ?: deriveLocation(exercise.equipment, exercise.name)

    fun movementTypeOf(exercise: ExerciseEntity): String =
        exercise.movementType
            ?: deriveMovementType(exercise.name, exercise.equipment, exercise.parts)

    fun difficultyOf(exercise: ExerciseEntity): String =
        exercise.difficulty ?: deriveDifficulty(exercise.name)

    fun deriveLocation(equipment: String?, name: String): String {
        val text = listOfNotNull(equipment, name).joinToString(" ")
        if (equipment.isNullOrBlank()) return LOCATION_ANY
        return when {
            text.contains("自重") -> LOCATION_ANY
            text.contains("槓") || text.contains("器械") || text.contains("滑輪") ||
                text.contains("史密斯") || text.contains("腿推") || text.contains("腿伸展") ||
                text.contains("腿彎舉") || text.contains("髖外展") || text.contains("雙槓") ||
                text.contains("單槓") -> LOCATION_GYM
            text.contains("啞鈴") || text.contains("壺鈴") || text.contains("彈力帶") ||
                text.contains("帶") || text.contains("TRX") || text.contains("水袋") ||
                text.contains("藥球") || text.contains("健身球") || text.contains("健腹輪") ||
                text.contains("跳繩") -> LOCATION_HOME
            Regex("\\d+\\s*(kg|公斤)", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                LOCATION_HOME
            else -> LOCATION_ANY
        }
    }

    fun deriveMovementType(name: String, equipment: String?, parts: String?): String {
        val text = listOfNotNull(name, parts).joinToString(" ")
        val gear = equipment.orEmpty()
        return when {
            text.contains("拉伸") || text.contains("平板支撐") ||
                text.contains("側棒式") || text.contains("棒式") ||
                (text.contains("伸展") && !text.contains("旋轉伸展") &&
                    !text.contains("三停") &&
                    (equipment.isNullOrBlank() || equipment.contains("自重"))) ->
                TYPE_STATIC
            text.contains("跑步") || text.contains("腳踏車") || text.contains("划船機") ||
                text.contains("橢圓機") || text.contains("踏步機") || text.contains("跳繩") ||
                text.contains("有氧") || text.contains("游泳") || text.contains("爬泳") ||
                text.contains("蛙泳") || text.contains("蝶泳") -> TYPE_CARDIO
            gear.contains("自重") ||
                (gear.contains("TRX") && !gear.contains("啞鈴") &&
                    !gear.contains("壺鈴") && !gear.contains("槓")) ||
                text.contains("伏地挺身") || text.contains("俯臥撐") ||
                text.contains("波比") || text.contains("開合跳") || text.contains("登山者") ||
                text.contains("引體向上") || text.contains("懸垂") -> TYPE_BODYWEIGHT
            else -> TYPE_WEIGHTED
        }
    }

    fun deriveDifficulty(name: String): String = when {
        name.contains("輔助") || name.contains("跪") || name.contains("牆") ||
            name.contains("初學") || name.contains("腿推") || name.contains("腿伸展") ||
            name.contains("腿彎舉") || name.contains("提踵") || name.contains("機械") ->
            DIFFICULTY_BEGINNER
        name.contains("單腿") || name.contains("爆發") || name.contains("進階") ||
            name.contains("倒立") || name.contains("前蹲") || name.contains("引體") ||
            name.contains("懸垂") || name.contains("早安") -> DIFFICULTY_ADVANCED
        else -> DIFFICULTY_INTERMEDIATE
    }

    /**
     * 替代動作：同分類優先，其次同型態、同器材；不足時用同型態補齊。
     */
    fun alternativesFor(
        current: ExerciseEntity,
        all: List<ExerciseEntity>
    ): List<ExerciseEntity> {
        val category = categoryOf(current)
        val type = movementTypeOf(current)
        val gear = equipmentCategory(current.equipment)
        val candidates = all.filter { it.id != current.id }
        val sameCategory = candidates.filter { categoryOf(it) == category }
        val sameType = candidates.filter { movementTypeOf(it) == type }
        return (sameCategory + sameType)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<ExerciseEntity> {
                    categoryOf(it) == category && movementTypeOf(it) == type
                }
                    .thenByDescending { movementTypeOf(it) == type }
                    .thenByDescending { categoryOf(it) == category }
                    .thenByDescending { equipmentCategory(it.equipment) == gear }
                    .thenBy { it.name }
            )
            .take(5)
    }
}
