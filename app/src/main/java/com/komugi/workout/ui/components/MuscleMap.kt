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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.workout.R

/**
 * 只取「最主要的訓練部位」：從部位文字中找出最早出現的關鍵字。
 */
enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ARMS, CORE,
    GLUTES, QUADS, HAMSTRINGS, CALVES, FULL_BODY
}

fun primaryMuscleGroup(parts: String?): MuscleGroup? {
    val text = parts?.trim().orEmpty()
    if (text.isEmpty()) return null
    if (text.contains("全身")) return MuscleGroup.FULL_BODY

    val keywords = listOf(
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

    return keywords
        .mapNotNull { (keyword, group) ->
            text.indexOf(keyword).takeIf { it >= 0 }?.let { it to group }
        }
        .minByOrNull { it.first }
        ?.second
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

@DrawableRes
private fun muscleGroupDrawable(group: MuscleGroup): Int = when (group) {
    MuscleGroup.CHEST -> R.drawable.muscle_chest
    MuscleGroup.BACK -> R.drawable.muscle_back
    MuscleGroup.SHOULDERS -> R.drawable.muscle_shoulders
    MuscleGroup.BICEPS -> R.drawable.muscle_biceps
    MuscleGroup.TRICEPS -> R.drawable.muscle_triceps
    MuscleGroup.FOREARMS -> R.drawable.muscle_forearms
    MuscleGroup.ARMS -> R.drawable.muscle_arms
    MuscleGroup.CORE -> R.drawable.muscle_core
    MuscleGroup.GLUTES -> R.drawable.muscle_glutes
    MuscleGroup.QUADS -> R.drawable.muscle_quads
    MuscleGroup.HAMSTRINGS -> R.drawable.muscle_hamstrings
    MuscleGroup.CALVES -> R.drawable.muscle_calves
    MuscleGroup.FULL_BODY -> R.drawable.muscle_full_body
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
