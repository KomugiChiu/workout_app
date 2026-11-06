package com.komugi.workout.ui.menu

import com.komugi.workout.data.MenuExerciseRow

/**
 * 可調三天循環的 A/B 天切換邏輯。
 *
 * 模板本體（cycle-flex）固定 7 個槽位，主幹 T1–T5 兩天共用；
 * A 天顯示 T1–T5＋F（心肺收尾），B 天顯示 H（啟動器）＋T1–T5。
 * 切換只影響當天顯示組合與 T4 組數，不動原模板；草稿以 slotKey 保留。
 */
enum class CycleFlexDay { A, B }

object CycleFlex {

    const val MENU_ID = "cycle-flex"

    const val SLOT_HEAD = "H"
    const val SLOT_T1 = "T1"
    const val SLOT_T2 = "T2"
    const val SLOT_T3 = "T3"
    const val SLOT_T4 = "T4"
    const val SLOT_T5 = "T5"
    const val SLOT_FINISH = "F"

    /** B 天分腿蹲給彈性組數（前面多了啟動器深蹲，允許降量）。 */
    const val T4_SETS_DAY_B = "2–3"

    fun activeSlots(day: CycleFlexDay): List<String> = when (day) {
        CycleFlexDay.A -> listOf(SLOT_T1, SLOT_T2, SLOT_T3, SLOT_T4, SLOT_T5, SLOT_FINISH)
        CycleFlexDay.B -> listOf(SLOT_HEAD, SLOT_T1, SLOT_T2, SLOT_T3, SLOT_T4, SLOT_T5)
    }

    fun isActive(slotKey: String?, day: CycleFlexDay): Boolean =
        slotKey != null && slotKey in activeSlots(day)

    /** 是否為本模板的槽位（他表單的列一律視為使用中，不受 A/B 影響）。 */
    fun isCycleSlot(slotKey: String?): Boolean = slotKey in knownSlots

    fun dayLabel(day: CycleFlexDay): String = when (day) {
        CycleFlexDay.A -> "A・肌力＋心肺"
        CycleFlexDay.B -> "B・純下肢肌力"
    }

    fun dayFromStored(value: String?): CycleFlexDay =
        if (value == "B") CycleFlexDay.B else CycleFlexDay.A

    fun dayToStored(day: CycleFlexDay): String = day.name

    private val knownSlots = setOf(
        SLOT_HEAD, SLOT_T1, SLOT_T2, SLOT_T3, SLOT_T4, SLOT_T5, SLOT_FINISH
    )

    /**
     * 套用當天組合：本模板的槽位只留當天 active；
     * T4 組數跟著當天走（B 天彈性 2–3 組、A 天還原模板值）。
     * 其他模板的列原樣保留。
     */
    fun applyDay(
        rows: List<MenuExerciseRow>,
        day: CycleFlexDay,
        templateRows: List<MenuExerciseRow> = emptyList()
    ): List<MenuExerciseRow> {
        val active = activeSlots(day).toSet()
        val templateT4Sets = templateRows.firstOrNull { it.slotKey == SLOT_T4 }?.targetSets
        return rows.mapNotNull { row ->
            val slot = row.slotKey
            if (slot != null && slot in knownSlots && slot !in active) {
                null
            } else if (slot == SLOT_T4) {
                val wanted = if (day == CycleFlexDay.B) T4_SETS_DAY_B else templateT4Sets
                if (wanted != null && row.targetSets != wanted) row.copy(targetSets = wanted) else row
            } else {
                row
            }
        }
    }
}
