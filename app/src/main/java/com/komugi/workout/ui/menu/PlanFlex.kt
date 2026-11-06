package com.komugi.workout.ui.menu

import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.MenuExerciseRow

/**
 * 可調重訓計畫的 A/B 天切換邏輯。
 *
 * 模板本體（plan-flex）固定 5 個槽位，DB 存的是 A 天預設；
 * B 天預設（動作、器材與角色）放在這裡，切換時套用。
 * 組數、休息、配對（2A＋2B、3A＋3B）兩天相同，不跟著換。
 * 兩天的已換動作分開記憶，切換不丟。
 */
enum class PlanFlexDay { A, B }

data class PlanDayDefault(
    val exerciseName: String,
    val equipment: String?,
    val role: String,
    val laterality: String,
    val pattern: String
)

object PlanFlex {

    const val MENU_ID = "plan-flex"

    const val SLOT_1 = "1"
    const val SLOT_2A = "2A"
    const val SLOT_2B = "2B"
    const val SLOT_3A = "3A"
    const val SLOT_3B = "3B"

    val slots = listOf(SLOT_1, SLOT_2A, SLOT_2B, SLOT_3A, SLOT_3B)

    /** B 天預設（A 天即模板本體，不另存）。 */
    val dayBDefaults: Map<String, PlanDayDefault> = mapOf(
        SLOT_1 to PlanDayDefault(
            exerciseName = "鳥狗式",
            equipment = "自重",
            role = "核心平衡",
            laterality = "通用",
            pattern = "核心"
        ),
        SLOT_2A to PlanDayDefault(
            exerciseName = "保加利亞分腿蹲",
            equipment = "4–6kg×2",
            role = "股四頭肌",
            laterality = "單邊",
            pattern = "蹲"
        ),
        SLOT_2B to PlanDayDefault(
            exerciseName = "滑輪下拉",
            equipment = "滑輪、高位下拉機",
            role = "背闊肌",
            laterality = "通用",
            pattern = "垂直拉"
        ),
        SLOT_3A to PlanDayDefault(
            exerciseName = "啞鈴 RDL",
            equipment = "10kg×2",
            role = "腿後肌與臀肌",
            laterality = "雙邊",
            pattern = "髖主導"
        ),
        SLOT_3B to PlanDayDefault(
            exerciseName = "阿諾德推舉",
            equipment = "啞鈴",
            role = "肩膀",
            laterality = "通用",
            pattern = "垂直推"
        )
    )

    fun dayLabel(day: PlanFlexDay): String = when (day) {
        PlanFlexDay.A -> "A・後鏈＋胸"
        PlanFlexDay.B -> "B・股四＋背闊＋肩"
    }

    fun dayFromStored(value: String?): PlanFlexDay =
        if (value == "B") PlanFlexDay.B else PlanFlexDay.A

    fun dayToStored(day: PlanFlexDay): String = day.name

    /** 上次動作記憶的命名空間（A/B 天分開記）。 */
    fun memoryNamespace(day: PlanFlexDay): String = "plan-flex-${day.name}"

    /**
     * 取當天模板列：A 天直接用模板本體；
     * B 天以模板列為骨架（順序、組數、休息、配對不變），
     * 換上 B 天預設動作、器材與角色。預設動作找不到時保留原列。
     */
    fun templateFor(
        templateRows: List<MenuExerciseRow>,
        day: PlanFlexDay,
        byName: (String) -> ExerciseEntity?
    ): List<MenuExerciseRow> {
        if (day == PlanFlexDay.A) return templateRows
        return templateRows.map { row ->
            val preset = dayBDefaults[row.slotKey] ?: return@map row
            val exercise = byName(preset.exerciseName) ?: return@map row
            row.copy(
                exerciseId = exercise.id,
                name = exercise.name,
                equipment = preset.equipment,
                parts = exercise.parts,
                videoUrl = exercise.videoUrl,
                guideId = exercise.guideId,
                role = preset.role,
                laterality = preset.laterality,
                pattern = preset.pattern
            )
        }
    }
}
