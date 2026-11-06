package com.komugi.workout.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.workout.R
import com.komugi.workout.ui.theme.AppTheme
import com.komugi.workout.ui.theme.LocalAppTheme

/**
 * 只取「最主要的訓練部位」：從部位文字中找出最早出現的關鍵字。
 */
enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ARMS, CORE,
    GLUTES, QUADS, HAMSTRINGS, CALVES, FULL_BODY
}

private val MuscleKeywords = listOf(
    "胸" to MuscleGroup.CHEST,
    "背闊" to MuscleGroup.BACK,
    "菱形" to MuscleGroup.BACK,
    "斜方" to MuscleGroup.BACK,
    "豎脊" to MuscleGroup.BACK,
    "三角" to MuscleGroup.SHOULDERS,
    "旋轉肌群" to MuscleGroup.SHOULDERS,
    "肱二頭" to MuscleGroup.BICEPS,
    "肱三頭" to MuscleGroup.TRICEPS,
    "肱橈肌" to MuscleGroup.FOREARMS,
    "握力" to MuscleGroup.FOREARMS,
    "前臂" to MuscleGroup.FOREARMS,
    "手臂" to MuscleGroup.ARMS,
    "腹" to MuscleGroup.CORE,
    "核心" to MuscleGroup.CORE,
    "臀" to MuscleGroup.GLUTES,
    "股四" to MuscleGroup.QUADS,
    "腿後" to MuscleGroup.HAMSTRINGS,
    "小腿" to MuscleGroup.CALVES
)

fun primaryMuscleGroup(parts: String?): MuscleGroup? {
    val text = parts?.trim().orEmpty()
    if (text.isEmpty()) return null
    if (text.contains("全身")) return MuscleGroup.FULL_BODY

    return MuscleKeywords
        .mapNotNull { (keyword, group) ->
            text.indexOf(keyword).takeIf { it >= 0 }?.let { it to group }
        }
        .minByOrNull { it.first }
        ?.second
}

/**
 * 列出文字中提到的所有主要部位（依出現順序、去重），最多 [limit] 個。
 */
fun muscleGroupsIn(parts: String?, limit: Int = 3): List<MuscleGroup> {
    val text = parts?.trim().orEmpty()
    if (text.isEmpty()) return emptyList()
    if (text.contains("全身")) return listOf(MuscleGroup.FULL_BODY)

    val hits = mutableListOf<Pair<Int, MuscleGroup>>()
    MuscleKeywords.forEach { (keyword, group) ->
        var index = text.indexOf(keyword)
        while (index >= 0) {
            hits.add(index to group)
            index = text.indexOf(keyword, index + keyword.length)
        }
    }
    return hits
        .sortedBy { it.first }
        .map { it.second }
        .distinct()
        .take(limit)
}

fun muscleGroupLabel(group: MuscleGroup): String = when (group) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.BACK -> "背"
    MuscleGroup.SHOULDERS -> "肩"
    MuscleGroup.BICEPS -> "二頭"
    MuscleGroup.TRICEPS -> "三頭"
    MuscleGroup.FOREARMS -> "前臂"
    MuscleGroup.ARMS -> "手臂"
    MuscleGroup.CORE -> "核心"
    MuscleGroup.GLUTES -> "臀"
    MuscleGroup.QUADS -> "股四"
    MuscleGroup.HAMSTRINGS -> "腿後"
    MuscleGroup.CALVES -> "小腿"
    MuscleGroup.FULL_BODY -> "全身"
}

/**
 * 是否算全身動作：部位文字含「全身」，或同時動用到
 * 上肢（胸背肩臂）＋下肢（臀腿）＋核心三區的複合動作。
 */
fun isFullBodyExercise(parts: String?): Boolean {
    val text = parts?.trim().orEmpty()
    if (text.isEmpty()) return false
    if (text.contains("全身")) return true
    val groups = muscleGroupsIn(text, limit = 10).toSet()
    val upper = setOf(
        MuscleGroup.CHEST,
        MuscleGroup.BACK,
        MuscleGroup.SHOULDERS,
        MuscleGroup.BICEPS,
        MuscleGroup.TRICEPS,
        MuscleGroup.FOREARMS,
        MuscleGroup.ARMS
    )
    val lower = setOf(
        MuscleGroup.GLUTES,
        MuscleGroup.QUADS,
        MuscleGroup.HAMSTRINGS,
        MuscleGroup.CALVES
    )
    return MuscleGroup.CORE in groups &&
        groups.any { it in upper } &&
        groups.any { it in lower }
}

/** 圖示視角：前側或後側 */
private fun muscleGroupView(group: MuscleGroup): String? = when (group) {
    MuscleGroup.CHEST,
    MuscleGroup.SHOULDERS,
    MuscleGroup.BICEPS,
    MuscleGroup.FOREARMS,
    MuscleGroup.CORE,
    MuscleGroup.QUADS -> "前"

    MuscleGroup.BACK,
    MuscleGroup.TRICEPS,
    MuscleGroup.GLUTES,
    MuscleGroup.HAMSTRINGS,
    MuscleGroup.CALVES -> "後"

    else -> null
}

/**
 * 依目前主題選擇對應高亮色的圖檔：森林綠／海洋藍／暖陽橘。
 * 圖檔命名規則：muscle_<部位>（森林綠）、_ocean、_sunset。
 */
@Composable
@DrawableRes
private fun muscleGroupDrawable(group: MuscleGroup): Int {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val suffix = when (theme) {
        AppTheme.FOREST -> ""
        AppTheme.OCEAN -> "_ocean"
        AppTheme.SUNSET -> "_sunset"
    }
    val resourceName = "muscle_${group.name.lowercase()}$suffix"
    return remember(resourceName) {
        context.resources
            .getIdentifier(resourceName, "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: R.drawable.muscle_full_body
    }
}

/**
 * 解剖局部肌群圖（擷取自開源人體肌群圖並局部裁切著色）。
 */
@Composable
fun MuscleMap(
    group: MuscleGroup,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val badgeColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val label = muscleGroupView(group)?.let { view ->
        "${muscleGroupLabel(group)}（$view）"
    } ?: muscleGroupLabel(group)

    Column(modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = muscleGroupDrawable(group)),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = accent
        )
    }
}
