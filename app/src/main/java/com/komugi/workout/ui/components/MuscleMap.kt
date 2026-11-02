package com.komugi.workout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

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

/**
 * 局部部位圖：只畫出該肌群所在的部位（例：二頭只畫手臂），
 * 灰色為身體、綠色為目標肌肉。
 */
@Composable
fun MuscleMap(
    group: MuscleGroup,
    modifier: Modifier = Modifier
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val accent = MaterialTheme.colorScheme.primary
    val badgeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)

    Column(modifier) {
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            val w = size.width
            val h = size.height

            fun px(v: Float): Float = v / 100f * w
            fun py(v: Float): Float = v / 140f * h
            fun sw(v: Float): Float = v / 100f * w

            fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Color) {
                drawLine(
                    color = color,
                    start = Offset(px(x1), py(y1)),
                    end = Offset(px(x2), py(y2)),
                    strokeWidth = sw(width),
                    cap = StrokeCap.Round
                )
            }

            fun circle(x: Float, y: Float, r: Float, color: Color) {
                drawCircle(color = color, radius = sw(r), center = Offset(px(x), py(y)))
            }

            fun oval(x: Float, y: Float, rx: Float, ry: Float, color: Color) {
                drawOval(
                    color = color,
                    topLeft = Offset(px(x - rx), py(y - ry)),
                    size = Size(px(rx * 2f), py(ry * 2f))
                )
            }

            fun rect(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, color: Color) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(px(x1), py(y1)),
                    size = Size(px(x2 - x1), py(y2 - y1)),
                    cornerRadius = CornerRadius(sw(radius), sw(radius))
                )
            }

            // 背景徽章
            drawRoundRect(
                color = badgeColor,
                topLeft = Offset(w * 0.04f, h * 0.02f),
                size = Size(w * 0.92f, h * 0.94f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
            )

            // 共用身體部位
            fun drawTorso() {
                circle(50f, 12f, 9f, base)
                line(50f, 20f, 50f, 30f, 8f, base)
                val torso = Path().apply {
                    moveTo(px(24f), py(30f))
                    lineTo(px(76f), py(30f))
                    lineTo(px(68f), py(82f))
                    lineTo(px(72f), py(120f))
                    lineTo(px(28f), py(120f))
                    lineTo(px(32f), py(82f))
                    close()
                }
                drawPath(torso, base)
                line(24f, 32f, 14f, 70f, 9f, base)
                line(76f, 32f, 86f, 70f, 9f, base)
            }

            fun drawArm() {
                // 彎曲手臂：肩 → 肘 → 腕
                circle(28f, 18f, 9f, base)
                line(28f, 26f, 58f, 70f, 20f, base)
                circle(58f, 70f, 10f, base)
                line(58f, 70f, 38f, 108f, 14f, base)
                circle(34f, 118f, 7f, base)
            }

            fun drawThigh() {
                circle(50f, 12f, 10f, base)
                line(50f, 20f, 50f, 82f, 26f, base)
                circle(50f, 86f, 11f, base)
                line(50f, 94f, 50f, 122f, 13f, base)
                oval(50f, 130f, 9f, 6f, base)
            }

            fun drawCalf() {
                circle(50f, 16f, 12f, base)
                line(50f, 26f, 50f, 100f, 15f, base)
                circle(50f, 104f, 7f, base)
                oval(50f, 116f, 10f, 6f, base)
            }

            fun drawHips() {
                val hips = Path().apply {
                    moveTo(px(30f), py(18f))
                    lineTo(px(70f), py(18f))
                    lineTo(px(74f), py(55f))
                    lineTo(px(62f), py(78f))
                    lineTo(px(38f), py(78f))
                    lineTo(px(26f), py(55f))
                    close()
                }
                drawPath(hips, base)
                line(40f, 76f, 36f, 120f, 16f, base)
                line(60f, 76f, 64f, 120f, 16f, base)
            }

            when (group) {
                MuscleGroup.CHEST -> {
                    drawTorso()
                    oval(40f, 55f, 10f, 12f, accent)
                    oval(60f, 55f, 10f, 12f, accent)
                    line(50f, 44f, 50f, 66f, 2f, badgeColor)
                }
                MuscleGroup.BACK -> {
                    drawTorso()
                    val back = Path().apply {
                        moveTo(px(28f), py(38f))
                        lineTo(px(72f), py(38f))
                        lineTo(px(62f), py(88f))
                        lineTo(px(50f), py(100f))
                        lineTo(px(38f), py(88f))
                        close()
                    }
                    drawPath(back, accent)
                    line(50f, 40f, 50f, 96f, 2f, badgeColor)
                }
                MuscleGroup.SHOULDERS -> {
                    drawTorso()
                    circle(24f, 33f, 9f, accent)
                    circle(76f, 33f, 9f, accent)
                }
                MuscleGroup.CORE -> {
                    drawTorso()
                    rect(38f, 66f, 62f, 108f, 6f, accent)
                    line(50f, 68f, 50f, 106f, 2f, badgeColor)
                    line(40f, 80f, 60f, 80f, 2f, badgeColor)
                    line(40f, 92f, 60f, 92f, 2f, badgeColor)
                }
                MuscleGroup.BICEPS -> {
                    drawArm()
                    line(30f, 38f, 54f, 64f, 11f, accent)
                }
                MuscleGroup.TRICEPS -> {
                    drawArm()
                    line(36f, 36f, 62f, 62f, 11f, accent)
                }
                MuscleGroup.FOREARMS -> {
                    drawArm()
                    line(56f, 76f, 40f, 104f, 9f, accent)
                }
                MuscleGroup.ARMS -> {
                    drawArm()
                    line(28f, 30f, 56f, 68f, 11f, accent)
                }
                MuscleGroup.GLUTES -> {
                    drawHips()
                    circle(40f, 58f, 13f, accent)
                    circle(60f, 58f, 13f, accent)
                    line(50f, 46f, 50f, 72f, 2f, badgeColor)
                }
                MuscleGroup.QUADS -> {
                    drawThigh()
                    oval(50f, 48f, 10f, 22f, accent)
                    // 股內側肌（膝內上方淚滴）
                    oval(44f, 72f, 4.5f, 8f, accent)
                }
                MuscleGroup.HAMSTRINGS -> {
                    drawThigh()
                    oval(44f, 52f, 6f, 21f, accent)
                    oval(56f, 52f, 6f, 21f, accent)
                }
                MuscleGroup.CALVES -> {
                    drawCalf()
                    oval(45f, 62f, 6f, 18f, accent)
                    oval(55f, 62f, 6f, 18f, accent)
                }
                MuscleGroup.FULL_BODY -> {
                    circle(50f, 12f, 9f, accent)
                    line(50f, 20f, 50f, 84f, 20f, accent)
                    line(50f, 42f, 22f, 66f, 9f, accent)
                    line(50f, 42f, 78f, 66f, 9f, accent)
                    line(46f, 82f, 38f, 128f, 12f, accent)
                    line(54f, 82f, 62f, 128f, 12f, accent)
                }
            }
        }

        Text(
            text = muscleGroupView(group)?.let { view ->
                "${muscleGroupLabel(group)}（$view）"
            } ?: muscleGroupLabel(group),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = accent
        )
    }
}
