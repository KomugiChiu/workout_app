package com.komugi.workout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap

/**
 * 只取「最主要的訓練部位」：從部位文字中找出最早出現的關鍵字。
 * 例如「臀大肌、腿後腱、股四頭」→ 臀；「胸大肌、前三角」→ 胸。
 */
enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, ARMS, CORE, GLUTES, QUADS, HAMSTRINGS, CALVES, FULL_BODY
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
        "肱二頭" to MuscleGroup.ARMS,
        "肱三頭" to MuscleGroup.ARMS,
        "握力" to MuscleGroup.ARMS,
        "前臂" to MuscleGroup.ARMS,
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

/**
 * 簡化人形部位圖：灰色身體 + 綠色主要部位。
 * 背面肌群（背／臀／腿後）會以背面示意。
 */
@Composable
fun MuscleMap(
    group: MuscleGroup,
    modifier: Modifier = Modifier
) {
    val baseBody = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val accent = MaterialTheme.colorScheme.primary

    val bodyColor: Color =
        if (group == MuscleGroup.FULL_BODY) accent else baseBody

    val isBack = group == MuscleGroup.BACK ||
        group == MuscleGroup.GLUTES ||
        group == MuscleGroup.HAMSTRINGS

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        fun px(v: Float): Float = v / 100f * w
        fun py(v: Float): Float = v / 140f * h
        fun sw(v: Float): Float = v / 100f * w

        // 腿
        drawLine(
            color = bodyColor,
            start = Offset(px(44f), py(86f)),
            end = Offset(px(42f), py(112f)),
            strokeWidth = sw(13f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(42f), py(112f)),
            end = Offset(px(40f), py(132f)),
            strokeWidth = sw(10f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(56f), py(86f)),
            end = Offset(px(58f), py(112f)),
            strokeWidth = sw(13f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(58f), py(112f)),
            end = Offset(px(60f), py(132f)),
            strokeWidth = sw(10f),
            cap = StrokeCap.Round
        )

        // 手臂
        drawLine(
            color = bodyColor,
            start = Offset(px(30f), py(40f)),
            end = Offset(px(22f), py(64f)),
            strokeWidth = sw(8f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(22f), py(64f)),
            end = Offset(px(18f), py(88f)),
            strokeWidth = sw(7f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(70f), py(40f)),
            end = Offset(px(78f), py(64f)),
            strokeWidth = sw(8f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(78f), py(64f)),
            end = Offset(px(82f), py(88f)),
            strokeWidth = sw(7f),
            cap = StrokeCap.Round
        )

        // 軀幹
        val torso = Path().apply {
            moveTo(px(30f), py(36f))
            lineTo(px(70f), py(36f))
            lineTo(px(64f), py(72f))
            lineTo(px(68f), py(86f))
            lineTo(px(32f), py(86f))
            lineTo(px(36f), py(72f))
            close()
        }
        drawPath(torso, bodyColor)

        // 頭＋頸
        drawCircle(
            color = bodyColor,
            radius = sw(9f),
            center = Offset(px(50f), py(18f))
        )
        drawLine(
            color = bodyColor,
            start = Offset(px(50f), py(26f)),
            end = Offset(px(50f), py(36f)),
            strokeWidth = sw(8f),
            cap = StrokeCap.Round
        )

        // 主要部位高亮
        when (group) {
            MuscleGroup.CHEST -> {
                drawCircle(accent, sw(7f), Offset(px(43f), py(51f)))
                drawCircle(accent, sw(7f), Offset(px(57f), py(51f)))
            }
            MuscleGroup.CORE -> drawRoundRect(
                color = accent,
                topLeft = Offset(px(39f), py(60f)),
                size = Size(px(22f), py(20f)),
                cornerRadius = CornerRadius(sw(6f), sw(6f))
            )
            MuscleGroup.SHOULDERS -> {
                drawCircle(accent, sw(6f), Offset(px(30f), py(40f)))
                drawCircle(accent, sw(6f), Offset(px(70f), py(40f)))
            }
            MuscleGroup.ARMS -> {
                drawLine(accent, Offset(px(30f), py(40f)), Offset(px(22f), py(64f)), sw(8.5f), StrokeCap.Round)
                drawLine(accent, Offset(px(70f), py(40f)), Offset(px(78f), py(64f)), sw(8.5f), StrokeCap.Round)
            }
            MuscleGroup.QUADS -> {
                drawLine(accent, Offset(px(44f), py(90f)), Offset(px(42f), py(110f)), sw(12f), StrokeCap.Round)
                drawLine(accent, Offset(px(56f), py(90f)), Offset(px(58f), py(110f)), sw(12f), StrokeCap.Round)
            }
            MuscleGroup.CALVES -> {
                drawLine(accent, Offset(px(42f), py(114f)), Offset(px(40f), py(130f)), sw(9f), StrokeCap.Round)
                drawLine(accent, Offset(px(58f), py(114f)), Offset(px(60f), py(130f)), sw(9f), StrokeCap.Round)
            }
            MuscleGroup.BACK -> {
                val back = Path().apply {
                    moveTo(px(32f), py(38f))
                    lineTo(px(68f), py(38f))
                    lineTo(px(64f), py(70f))
                    lineTo(px(36f), py(70f))
                    close()
                }
                drawPath(back, accent)
            }
            MuscleGroup.GLUTES -> {
                drawCircle(accent, sw(7.5f), Offset(px(43f), py(84f)))
                drawCircle(accent, sw(7.5f), Offset(px(57f), py(84f)))
            }
            MuscleGroup.HAMSTRINGS -> {
                drawLine(accent, Offset(px(44f), py(92f)), Offset(px(42f), py(112f)), sw(12f), StrokeCap.Round)
                drawLine(accent, Offset(px(56f), py(92f)), Offset(px(58f), py(112f)), sw(12f), StrokeCap.Round)
            }
            MuscleGroup.FULL_BODY -> Unit
        }

        if (isBack) {
            // 背面示意：淡淡標一個「背」的記號
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = sw(1.6f),
                center = Offset(px(50f), py(34f))
            )
        }
    }
}
