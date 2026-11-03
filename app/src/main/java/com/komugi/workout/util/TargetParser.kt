package com.komugi.workout.util

/**
 * 從菜單的建議文字中解析數值，例如：
 * - targetSets "3–4"   -> 3
 * - targetReps "8–10"  -> 8
 * - targetReps "30 秒" -> duration 30
 * - equipment  "10kg×2" -> 10
 */
object TargetParser {

    private val startNumber = Regex("^(\\d+(?:\\.\\d+)?)")

    fun firstNumberAtStart(text: String?): Double? =
        text?.trim()?.let { startNumber.find(it)?.groupValues?.get(1)?.toDoubleOrNull() }

    fun kgFromEquipment(text: String?): Double? =
        Regex("(\\d+(?:\\.\\d+)?)\\s*kg", RegexOption.IGNORE_CASE)
            .find(text ?: "")
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()

    fun seconds(text: String?): Int? =
        if (text?.contains("秒") == true) firstNumberAtStart(text)?.toInt() else null

    /**
     * 從「90s」「60–90s」「休 60」等文字取出休息秒數，預設限制在 10–600 秒。
     */
    fun restSeconds(text: String?): Int? =
        Regex("\\d+")
            .find(text ?: "")
            ?.value
            ?.toIntOrNull()
            ?.coerceIn(10, 600)
}
