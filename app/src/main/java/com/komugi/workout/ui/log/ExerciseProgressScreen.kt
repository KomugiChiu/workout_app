package com.komugi.workout.ui.log

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.EntryEntity
import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.ExerciseSetRow
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.formatEpochDay
import com.komugi.workout.util.formatWeight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.flowOf

private data class ExerciseSession(
    val sessionId: Long,
    val dateEpochDay: Long,
    val menuName: String?,
    val sets: List<ExerciseSetRow>,
    val totalReps: Int,
    val totalDuration: Int,
    val volume: Double,
    val maxWeight: Double?
)

private enum class ProgressPeriod(val label: String) {
    DAY("日"),
    WEEK("週"),
    MONTH("月")
}

private enum class ProgressMetric(val label: String) {
    MAX_WEIGHT("最高重量"),
    VOLUME("訓練量"),
    REPS("總次數"),
    DURATION("總時間")
}

private data class ChartPoint(
    val key: Long,
    val label: String,
    val value: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    repository: WorkoutRepository,
    entryId: Long,
    onBack: () -> Unit
) {
    val entry by produceState<EntryEntity?>(initialValue = null, entryId) {
        value = repository.entryById(entryId)
    }
    val exercise by produceState<ExerciseEntity?>(initialValue = null, entry?.name) {
        value = entry?.name?.let { repository.exerciseByName(it) }
    }
    val historyFlow = remember(entry?.name, entry?.exerciseId) {
        entry?.let { repository.observeExerciseSets(it.exerciseId, it.name) }
    }
    val rows by (historyFlow ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val sessions = remember(rows) { buildSessions(rows) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動作進度") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val currentEntry = entry
        if (currentEntry == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val metrics = remember(sessions) { availableMetrics(sessions) }
        var metric by remember { mutableStateOf(ProgressMetric.MAX_WEIGHT) }
        var metricPicked by remember { mutableStateOf(false) }
        var period by remember { mutableStateOf(ProgressPeriod.DAY) }
        LaunchedEffect(metrics, metricPicked) {
            if (!metricPicked || metric !in metrics) {
                metric = metrics.first()
            }
        }

        val points = remember(sessions, period, metric) {
            buildChartPoints(sessions, period, metric)
        }
        val totalReps = rows.sumOf { it.reps ?: 0 }
        val totalVolume = sessions.sumOf { it.volume }
        val totalDuration = sessions.sumOf { it.totalDuration }
        val bestReps = rows.maxOfOrNull { it.reps ?: 0 } ?: 0
        val pr = sessions.mapNotNull { it.maxWeight }.maxOrNull()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExerciseHeaderCard(
                    entry = currentEntry,
                    exercise = exercise,
                    sessions = sessions,
                    totalSets = rows.size
                )
            }
            item {
                ExerciseStatsCard(
                    sessions = sessions,
                    totalSets = rows.size,
                    totalReps = totalReps,
                    totalVolume = totalVolume,
                    totalDuration = totalDuration,
                    bestReps = bestReps,
                    pr = pr
                )
            }
            item {
                ProgressChartCard(
                    sessions = sessions,
                    metrics = metrics,
                    metric = metric,
                    onMetricChange = {
                        metric = it
                        metricPicked = true
                    },
                    period = period,
                    onPeriodChange = { period = it },
                    points = points
                )
            }
            item {
                Text(
                    "訓練歷史",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (sessions.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("還沒有這個動作的訓練紀錄", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "先到紀錄頁新增組數，這裡就會出現歷史與進度。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sessions.asReversed(), key = { it.sessionId }) { session ->
                    SessionHistoryCard(session = session, pr = pr)
                }
            }
            item {
                Text(
                    "以動作名稱合併新舊紀錄；舊資料沒有動作 ID 也能納入。" +
                        "訓練量＝重量 × 次數，自重不計。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseHeaderCard(
    entry: EntryEntity,
    exercise: ExerciseEntity?,
    sessions: List<ExerciseSession>,
    totalSets: Int
) {
    val parts = entry.targetParts ?: exercise?.parts
    val meta = listOfNotNull(parts, entry.equipment).joinToString(" · ")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.name, style = MaterialTheme.typography.headlineSmall)
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${sessions.size} 次訓練 · $totalSets 組",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ExerciseStatsCard(
    sessions: List<ExerciseSession>,
    totalSets: Int,
    totalReps: Int,
    totalVolume: Double,
    totalDuration: Int,
    bestReps: Int,
    pr: Double?
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row {
                ProgressStat(
                    label = "訓練次數",
                    value = "${sessions.size}",
                    modifier = Modifier.weight(1f)
                )
                ProgressStat(
                    label = "總組數",
                    value = "$totalSets",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                ProgressStat(
                    label = "總次數",
                    value = "$totalReps 下",
                    modifier = Modifier.weight(1f)
                )
                ProgressStat(
                    label = when {
                        pr != null -> "最佳重量"
                        totalDuration > 0 -> "總時間"
                        bestReps > 0 -> "最佳單組"
                        totalVolume > 0 -> "總訓練量"
                        else -> "最佳單組"
                    },
                    value = when {
                        pr != null -> "${formatWeight(pr)} kg"
                        totalDuration > 0 -> formatDurationValue(totalDuration)
                        bestReps > 0 -> "$bestReps 下"
                        totalVolume > 0 -> "${formatVolumeValue(totalVolume)} kg"
                        else -> "—"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            val extras = buildList {
                if (pr != null && totalVolume > 0) {
                    add("累計訓練量 ${formatVolumeValue(totalVolume)} kg")
                }
                if (totalDuration > 0) {
                    add("累計時間 ${formatDurationValue(totalDuration)}")
                }
            }
            if (extras.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    extras.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProgressStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressChartCard(
    sessions: List<ExerciseSession>,
    metrics: List<ProgressMetric>,
    metric: ProgressMetric,
    onMetricChange: (ProgressMetric) -> Unit,
    period: ProgressPeriod,
    onPeriodChange: (ProgressPeriod) -> Unit,
    points: List<ChartPoint>
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${metric.label}趨勢",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { item ->
                    FilterChip(
                        selected = metric == item,
                        onClick = { onMetricChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressPeriod.entries.forEach { item ->
                    FilterChip(
                        selected = period == item,
                        onClick = { onPeriodChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (points.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "此區間尚無資料",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ProgressChart(points = points, metric = metric)
            }

            val withData = sessions.filter { metricValue(it, metric) > 0 }
            val last = withData.lastOrNull()
            if (last != null) {
                val previous = withData.dropLast(1).lastOrNull()
                val delta = deltaText(last, previous, metric)
                Text(
                    "最近一次 ${formatMetric(metricValue(last, metric), metric)}" +
                        (delta?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                "顯示最近 16 個區間",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ProgressChart(points: List<ChartPoint>, metric: ProgressMetric) {
    val maxValue = maxOf(points.maxOf { it.value }, 1.0)
    val primary = MaterialTheme.colorScheme.primary
    val pointInner = MaterialTheme.colorScheme.surface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Column(
                Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    formatMetric(maxValue, metric),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
            Spacer(Modifier.width(8.dp))
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val width = size.width
                val height = size.height
                val topInset = 8.dp.toPx()
                val bottomInset = 8.dp.toPx()
                val chartHeight = (height - topInset - bottomInset).coerceAtLeast(1f)

                fun pointY(value: Double): Float =
                    topInset + chartHeight * (1f - (value / maxValue).toFloat())

                repeat(3) { index ->
                    val y = topInset + chartHeight * index / 2f
                    drawLine(
                        color = grid,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size == 1) {
                    val y = pointY(points.first().value)
                    drawCircle(pointInner, 6.dp.toPx(), Offset(width / 2f, y))
                    drawCircle(primary, 4.dp.toPx(), Offset(width / 2f, y))
                } else {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = index * width / (points.size - 1)
                        val y = pointY(point.value)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = primary,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    points.forEachIndexed { index, point ->
                        val x = index * width / (points.size - 1)
                        val y = pointY(point.value)
                        drawCircle(pointInner, 6.dp.toPx(), Offset(x, y))
                        drawCircle(primary, 4.dp.toPx(), Offset(x, y))
                    }
                }
            }
        }
        if (points.size == 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 84.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    points.first().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 84.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    points.first().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
                if (points.size > 2) {
                    Text(
                        points[points.size / 2].label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
                Text(
                    points.last().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(session: ExerciseSession, pr: Double?) {
    var expanded by remember(session.sessionId) { mutableStateOf(false) }
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        formatEpochDay(session.dateEpochDay),
                        style = MaterialTheme.typography.titleMedium
                    )
                    session.menuName?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (pr != null && session.maxWeight != null && session.maxWeight == pr) {
                    Text(
                        "PR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    "${session.sets.size} 組",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(4.dp))
            val summary = buildList {
                if (session.totalReps > 0) add("${session.totalReps} 下")
                session.maxWeight?.let { add("最高 ${formatWeight(it)} kg") }
                if (session.volume > 0) add("訓練量 ${formatVolumeValue(session.volume)} kg")
                if (session.totalDuration > 0) add(formatDurationValue(session.totalDuration))
            }.joinToString(" · ")
            if (summary.isNotBlank()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                session.sets.forEachIndexed { index, set ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            "${index + 1}",
                            modifier = Modifier.width(28.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            setLabel(set),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Text(
                if (expanded) "收合" else "展開組數",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun buildSessions(rows: List<ExerciseSetRow>): List<ExerciseSession> =
    rows.groupBy { it.sessionId }
        .map { (sessionId, list) ->
            ExerciseSession(
                sessionId = sessionId,
                dateEpochDay = list.first().dateEpochDay,
                menuName = list.first().menuName,
                sets = list,
                totalReps = list.sumOf { it.reps ?: 0 },
                totalDuration = list.sumOf { it.durationSec ?: 0 },
                volume = list.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
                maxWeight = list.mapNotNull { it.weightKg }.maxOrNull()
            )
        }
        .sortedWith(compareBy({ it.dateEpochDay }, { it.sessionId }))

private fun availableMetrics(sessions: List<ExerciseSession>): List<ProgressMetric> {
    val hasWeight = sessions.any { it.maxWeight != null }
    val hasReps = sessions.any { it.totalReps > 0 }
    val hasDuration = sessions.any { it.totalDuration > 0 }
    return buildList {
        if (hasWeight) {
            add(ProgressMetric.MAX_WEIGHT)
            add(ProgressMetric.VOLUME)
        }
        if (hasReps) add(ProgressMetric.REPS)
        if (hasDuration) add(ProgressMetric.DURATION)
    }.ifEmpty { listOf(ProgressMetric.REPS) }
}

private fun metricValue(session: ExerciseSession, metric: ProgressMetric): Double = when (metric) {
    ProgressMetric.MAX_WEIGHT -> session.maxWeight ?: 0.0
    ProgressMetric.VOLUME -> session.volume
    ProgressMetric.REPS -> session.totalReps.toDouble()
    ProgressMetric.DURATION -> session.totalDuration.toDouble()
}

private fun buildChartPoints(
    sessions: List<ExerciseSession>,
    period: ProgressPeriod,
    metric: ProgressMetric
): List<ChartPoint> {
    val buckets = LinkedHashMap<Long, MutableList<ExerciseSession>>()
    sessions.forEach { session ->
        val date = LocalDate.ofEpochDay(session.dateEpochDay)
        val key = when (period) {
            ProgressPeriod.DAY -> session.dateEpochDay
            ProgressPeriod.WEEK -> date.with(DayOfWeek.MONDAY).toEpochDay()
            ProgressPeriod.MONTH -> date.year * 12L + (date.monthValue - 1)
        }
        buckets.getOrPut(key) { mutableListOf() }.add(session)
    }

    return buckets.map { (key, list) ->
        val value = when (metric) {
            ProgressMetric.MAX_WEIGHT -> list.mapNotNull { it.maxWeight }.maxOrNull() ?: 0.0
            ProgressMetric.VOLUME -> list.sumOf { it.volume }
            ProgressMetric.REPS -> list.sumOf { it.totalReps }.toDouble()
            ProgressMetric.DURATION -> list.sumOf { it.totalDuration }.toDouble()
        }
        ChartPoint(
            key = key,
            label = periodLabel(key, period),
            value = value
        )
    }
        .filter { it.value > 0 }
        .sortedBy { it.key }
        .takeLast(16)
}

private fun periodLabel(key: Long, period: ProgressPeriod): String = when (period) {
    ProgressPeriod.DAY, ProgressPeriod.WEEK ->
        LocalDate.ofEpochDay(key).format(DateTimeFormatter.ofPattern("M/d"))
    ProgressPeriod.MONTH -> {
        val year = key / 12
        val month = key % 12 + 1
        "$year/$month"
    }
}

private fun deltaText(
    last: ExerciseSession,
    previous: ExerciseSession?,
    metric: ProgressMetric
): String? {
    if (previous == null) return "第一次紀錄"
    val lastValue = metricValue(last, metric)
    val previousValue = metricValue(previous, metric)
    if (previousValue <= 0) return null
    val delta = lastValue - previousValue
    return when {
        delta > 0 -> "較上次 +${formatMetric(delta, metric)}"
        delta < 0 -> "較上次 -${formatMetric(-delta, metric)}"
        else -> "與上次相同"
    }
}

private fun formatMetric(value: Double, metric: ProgressMetric): String = when (metric) {
    ProgressMetric.MAX_WEIGHT -> "${formatWeight(value)} kg"
    ProgressMetric.VOLUME -> "${formatVolumeValue(value)} kg"
    ProgressMetric.REPS -> "${value.roundToInt()} 下"
    ProgressMetric.DURATION -> formatDurationValue(value.roundToInt())
}

private fun formatVolumeValue(value: Double): String =
    String.format(Locale.US, "%,d", value.roundToLong())

private fun formatDurationValue(seconds: Int): String =
    if (seconds >= 60) {
        "${seconds / 60} 分 ${seconds % 60} 秒"
    } else {
        "$seconds 秒"
    }

private fun setLabel(set: ExerciseSetRow): String {
    val parts = buildList {
        if (set.isBodyweight) {
            add("自重")
        } else {
            set.weightKg?.let { add("${formatWeight(it)} kg") }
        }
        set.reps?.let { add("$it 下") }
        set.durationSec?.let { add("$it 秒") }
    }
    return if (parts.isEmpty()) "—" else parts.joinToString(" × ")
}
