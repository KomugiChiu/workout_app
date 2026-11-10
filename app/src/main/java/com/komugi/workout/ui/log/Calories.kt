package com.komugi.workout.ui.log

import com.komugi.workout.data.BodyMetricEntity
import com.komugi.workout.data.EntryUi
import com.komugi.workout.data.SetEntity
import com.komugi.workout.util.TargetParser

/**
 * 組間休息與動作的熱量估算（MET 法）。
 *
 * kcal = MET × 體重(kg) × 時間(hr)
 *
 * 時間由實際紀錄推算：有秒數的動作（平板支撐等）用 durationSec，
 * 次數型的用「每下 4 秒」估動作時間；休息段落用休息秒數 ×（組數−1），
 * 並以較低的休息 MET 計入。離線即可計算，數字為估算值。
 */
object Calories {

    /** 動作每下估計秒數（向心＋離心＋短暫停頓）。 */
    const val SECONDS_PER_REP = 4.0

    /** 休息／組間的 MET（低強度活動）。 */
    const val REST_MET = 1.5

    /** 找不到體重記錄時的備援（與身體數據種子值一致）。 */
    const val FALLBACK_WEIGHT_KG = 70.1

    /** 沒有部位資訊時的一般阻力訓練 MET。 */
    const val DEFAULT_MET = 4.0

    /**
     * 依部位字串給 MET。判斷順序：心肺 → 下肢／全身複合 → 大肌群上肢 →
     * 核心 → 單關節孤立 → 預設。
     */
    fun metForParts(parts: String?): Double {
        val text = parts ?: return DEFAULT_MET
        return when {
            text.contains("心肺") -> 7.0
            text.contains("股四頭") || text.contains("臀大肌") ||
                text.contains("腿後腱") || text.contains("全身") -> 5.5
            text.contains("胸大肌") || text.contains("背闊肌") ||
                text.contains("上背") || text.contains("菱形") ||
                text.contains("斜方") || text.contains("後三角") -> 4.5
            text.contains("腹") || text.contains("核心") -> 3.8
            text.contains("肱二頭") || text.contains("肱三頭") ||
                text.contains("三角肌") || text.contains("小腿") ||
                text.contains("前臂") || text.contains("肩") -> 3.5
            else -> DEFAULT_MET
        }
    }

    /** 單組的動作時間（秒）：優先實際計時，否則用次數 × 每下秒數。 */
    fun workSeconds(set: SetEntity): Int =
        set.durationSec?.takeIf { it > 0 }
            ?: ((set.reps ?: 0) * SECONDS_PER_REP).toInt()

    /** 動作的休息總時間（秒）：組間休息 ×（組數−1）。「0」＝直上，不計休息。 */
    fun restSeconds(entry: EntryUi): Int {
        if (TargetParser.firstNumberAtStart(entry.entry.restSec) == 0.0) return 0
        val perRest = TargetParser.restSeconds(entry.entry.restSec) ?: return 0
        val gaps = (entry.sets.size - 1).coerceAtLeast(0)
        return perRest * gaps
    }

    /** 單一動作的估算熱量（kcal）。 */
    fun entryKcal(entry: EntryUi, weightKg: Double): Double {
        if (entry.sets.isEmpty()) return 0.0
        val weight = weightKg.takeIf { it > 0 } ?: FALLBACK_WEIGHT_KG
        val met = metForParts(entry.entry.targetParts)
        val workHours = entry.sets.sumOf { workSeconds(it) } / 3600.0
        val restHours = restSeconds(entry) / 3600.0
        return met * weight * workHours + REST_MET * weight * restHours
    }

    /** 整筆訓練的估算熱量（kcal）。 */
    fun sessionKcal(entries: List<EntryUi>, weightKg: Double): Double =
        entries.sumOf { entryKcal(it, weightKg) }

    /** 顯示用：無條件進位到整數，至少顯示 1 kcal（有紀錄時）。 */
    fun displayKcal(kcal: Double): Int =
        if (kcal <= 0.0) 0 else kotlin.math.ceil(kcal).toInt()

    /** 依訓練日期挑體重：當天或之前最近一次；都沒有就取最早一筆；全無則備援值。 */
    fun weightForDate(metrics: List<BodyMetricEntity>, dateEpochDay: Long): Double {
        val withWeight = metrics.filter { (it.weightKg ?: 0.0) > 0.0 }
        if (withWeight.isEmpty()) return FALLBACK_WEIGHT_KG
        return withWeight.filter { it.dateEpochDay <= dateEpochDay }.maxByOrNull { it.dateEpochDay }
            ?.weightKg
            ?: withWeight.minByOrNull { it.dateEpochDay }?.weightKg
            ?: FALLBACK_WEIGHT_KG
    }
}
