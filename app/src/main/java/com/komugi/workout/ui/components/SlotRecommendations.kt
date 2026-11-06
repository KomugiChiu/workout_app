package com.komugi.workout.ui.components

/**
 * 每個課表槽位的推薦替代動作（依優先順序）。
 *
 * 這裡只放「推薦名單」，實際顯示時仍會依今天器材、是否符合角色與是否已使用過濾。
 * 名稱需與 assets/menus.json、exercises_extra.json 的動作名稱一致。
 */
object SlotRecommendations {

    private fun key(role: String, laterality: String?, pattern: String?): String =
        "$role|${laterality.orEmpty()}|${pattern.orEmpty()}"

    private val exact: Map<String, List<String>> = mapOf(
        key("核心穩定", ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_CORE) to listOf(
            "平板支撐", "死蟲式", "側平板支撐", "鳥狗式", "登山者"
        ),
        key("核心平衡", ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_CORE) to listOf(
            "鳥狗式", "死蟲式", "平板支撐", "側平板支撐", "登山者"
        ),
        key("腿後肌與臀肌", ExerciseRole.LATERALITY_SINGLE, ExerciseRole.PATTERN_HINGE) to listOf(
            "單手壺鈴 RDL", "單腳啞鈴 RDL", "單腳登階", "壺鈴登階", "保加利亞分腿蹲"
        ),
        key("上背肌", ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_PULL_HORIZONTAL) to listOf(
            "單手啞鈴划船", "單壺鈴彎身划船", "坐姿帶划船", "坐姿滑輪划船", "槓鈴划船"
        ),
        key("股四頭肌", ExerciseRole.LATERALITY_BILATERAL, ExerciseRole.PATTERN_SQUAT) to listOf(
            "高腳杯深蹲", "啞鈴深蹲", "槓鈴背蹲舉", "前蹲舉", "腿推機", "踩帶深蹲"
        ),
        key("胸肌", ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_PUSH_HORIZONTAL) to listOf(
            "中性握地板臥推", "伏地挺身", "上斜伏地挺身", "帶子臥推模擬", "槓鈴臥推", "滑輪夾胸"
        ),
        key("股四頭肌", ExerciseRole.LATERALITY_SINGLE, ExerciseRole.PATTERN_SQUAT) to listOf(
            "保加利亞分腿蹲", "持槓片分腿蹲", "單腳登階", "TRX 輔助單腳深蹲", "壺鈴登階"
        ),
        key("背闊肌", ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_PULL_VERTICAL) to listOf(
            "滑輪下拉", "引體向上", "直臂下拉", "直臂啞鈴上拉"
        ),
        key("腿後肌與臀肌", ExerciseRole.LATERALITY_BILATERAL, ExerciseRole.PATTERN_HINGE) to listOf(
            "啞鈴 RDL", "羅馬尼亞硬舉", "六角槓硬舉", "壺鈴擺盪", "槓鈴臀推", "腿彎舉機"
        ),
        key("肩膀", ExerciseRole.LATERALITY_ANY, ExerciseRole.PATTERN_PUSH_VERTICAL) to listOf(
            "啞鈴肩推", "阿諾德推舉", "站姿槓鈴肩推", "單手壺鈴肩推",
            "側平舉", "啞鈴反向飛鳥", "滑輪臉拉"
        ),
        key(ExerciseRole.ROLE_LOWER, ExerciseRole.LATERALITY_ANY, null) to listOf(
            "高腳杯深蹲", "啞鈴深蹲", "槓鈴背蹲舉", "前蹲舉", "保加利亞分腿蹲",
            "啞鈴 RDL", "羅馬尼亞硬舉", "六角槓硬舉", "單腳登階", "腿推機",
            "踩帶深蹲", "壺鈴登階"
        ),
        key(ExerciseRole.ROLE_PULL, ExerciseRole.LATERALITY_ANY, null) to listOf(
            "單手啞鈴划船", "坐姿滑輪划船", "滑輪下拉", "引體向上", "槓鈴划船",
            "坐姿帶划船", "直臂下拉", "單壺鈴彎身划船"
        ),
        key(ExerciseRole.ROLE_PUSH, ExerciseRole.LATERALITY_ANY, null) to listOf(
            "中性握地板臥推", "槓鈴臥推", "伏地挺身", "上斜伏地挺身", "啞鈴肩推",
            "阿諾德推舉", "站姿槓鈴肩推", "滑輪夾胸", "帶子臥推模擬"
        )
    )

    private val byRole: Map<String, List<String>> = exact.entries
        .groupBy { it.key.substringBefore('|') }
        .mapValues { (_, entries) -> entries.flatMap { it.value }.distinct() }

    fun namesFor(requirement: SlotRequirement): List<String> {
        val exactMatch = exact[key(requirement.role, requirement.laterality, requirement.pattern)]
        if (exactMatch != null) return exactMatch
        val withoutPattern = exact[key(requirement.role, requirement.laterality, null)]
        if (withoutPattern != null) return withoutPattern
        return byRole[requirement.role].orEmpty()
    }

    fun namesFor(role: String?, laterality: String?, pattern: String?): List<String> {
        if (role.isNullOrBlank()) return emptyList()
        return namesFor(SlotRequirement(role, laterality, pattern))
    }
}
