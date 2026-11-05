package com.komugi.workout.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.BodyMetricEntity
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.formatEpochDay
import com.komugi.workout.util.formatWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private enum class BodyMetricType(val label: String) {
    WEIGHT("體重"),
    FAT("體脂肪")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(repository: WorkoutRepository) {
    val metrics by repository.observeBodyMetrics().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BodyMetricEntity?>(null) }
    var deleting by remember { mutableStateOf<BodyMetricEntity?>(null) }
    var chartType by remember { mutableStateOf(BodyMetricType.WEIGHT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("身體數據") },
                colors = komugiTopAppBarColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新增紀錄") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LatestBodyCard(metrics) }
            item {
                BodyChartCard(
                    metrics = metrics,
                    type = chartType,
                    onTypeChange = { chartType = it }
                )
            }
            item {
                Text(
                    "歷史紀錄",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (metrics.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("還沒有身體數據", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "按右下「新增紀錄」開始記錄體重與體脂肪。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    metrics.sortedByDescending { it.dateEpochDay },
                    key = { it.id }
                ) { metric ->
                    BodyMetricRow(
                        metric = metric,
                        onEdit = {
                            editing = metric
                            showDialog = true
                        },
                        onDelete = { deleting = metric }
                    )
                }
            }
        }
    }

    if (showDialog) {
        BodyMetricDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { epochDay, weight, fat ->
                val id = editing?.id ?: 0L
                showDialog = false
                scope.launch {
                    repository.saveBodyMetric(id, epochDay, weight, fat)
                }
            }
        )
    }

    deleting?.let { metric ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("刪除這筆身體數據？") },
            text = { Text(formatEpochDay(metric.dateEpochDay)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { repository.deleteBodyMetric(metric) }
                        deleting = null
                    }
                ) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun LatestBodyCard(metrics: List<BodyMetricEntity>) {
    val latest = metrics.maxByOrNull { it.dateEpochDay }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (latest == null) {
                Text("最新數據", style = MaterialTheme.typography.titleMedium)
                Text(
                    "尚未記錄",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            Text("最新數據", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                BodyValue(
                    label = "體重",
                    value = latest.weightKg?.let { "${formatWeight(it)} kg" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                BodyValue(
                    label = "體脂肪",
                    value = latest.bodyFatPercent?.let { formatBodyFat(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                formatEpochDay(latest.dateEpochDay),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BodyValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BodyChartCard(
    metrics: List<BodyMetricEntity>,
    type: BodyMetricType,
    onTypeChange: (BodyMetricType) -> Unit
) {
    val points = metrics
        .mapNotNull { metric ->
            val value = when (type) {
                BodyMetricType.WEIGHT -> metric.weightKg
                BodyMetricType.FAT -> metric.bodyFatPercent
            }
            value?.let { metric.dateEpochDay to it }
        }
        .sortedBy { it.first }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${type.label}趨勢",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == BodyMetricType.WEIGHT,
                    onClick = { onTypeChange(BodyMetricType.WEIGHT) },
                    label = { Text("體重") }
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = type == BodyMetricType.FAT,
                    onClick = { onTypeChange(BodyMetricType.FAT) },
                    label = { Text("體脂肪") }
                )
            }
            Spacer(Modifier.height(8.dp))
            if (points.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "此項目尚無資料",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BodyLineChart(points = points, type = type)
            }
        }
    }
}

@Composable
private fun BodyLineChart(
    points: List<Pair<Long, Double>>,
    type: BodyMetricType
) {
    val rawMax = points.maxOf { it.second }
    val rawMin = points.minOf { it.second }
    val spread = (rawMax - rawMin).takeIf { it > 0.01 } ?: 1.0
    val padding = spread * 0.15
    val chartMax = rawMax + padding
    val chartMin = rawMin - padding
    val range = (chartMax - chartMin).coerceAtLeast(0.1)
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pointInner = MaterialTheme.colorScheme.surface

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
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
                    formatChartValue(chartMax, type),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    formatChartValue(chartMin, type),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
            ) {
                val width = size.width
                val height = size.height

                fun pointY(value: Double): Float =
                    (height * (1f - ((value - chartMin) / range))).toFloat()

                repeat(3) { index ->
                    val y = height * index / 2f
                    drawLine(
                        color = grid,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size == 1) {
                    val y = pointY(points.first().second)
                    drawCircle(pointInner, 6.dp.toPx(), Offset(width / 2f, y))
                    drawCircle(primary, 4.dp.toPx(), Offset(width / 2f, y))
                } else {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = index * width / (points.size - 1)
                        val y = pointY(point.second)
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
                        val y = pointY(point.second)
                        drawCircle(pointInner, 6.dp.toPx(), Offset(x, y))
                        drawCircle(primary, 4.dp.toPx(), Offset(x, y))
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 84.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (points.size == 1) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        chartDate(points.first().first),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            } else {
                Text(
                    chartDate(points.first().first),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
                if (points.size > 2) {
                    Text(
                        chartDate(points[points.size / 2].first),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
                Text(
                    chartDate(points.last().first),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun BodyMetricRow(
    metric: BodyMetricEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatEpochDay(metric.dateEpochDay),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    buildString {
                        append("體重 ")
                        append(metric.weightKg?.let { "${formatWeight(it)} kg" } ?: "—")
                        append(" · 體脂肪 ")
                        append(metric.bodyFatPercent?.let { formatBodyFat(it) } ?: "—")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onEdit) { Text("編輯") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyMetricDialog(
    initial: BodyMetricEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, Double?, Double?) -> Unit
) {
    var epochDay by remember {
        mutableStateOf(initial?.dateEpochDay ?: LocalDate.now().toEpochDay())
    }
    var weightText by remember {
        mutableStateOf(initial?.weightKg?.let { formatWeight(it) } ?: "")
    }
    var fatText by remember {
        mutableStateOf(initial?.bodyFatPercent?.let { formatBodyFatNumber(it) } ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val weight = weightText.trim().toDoubleOrNull()
    val fat = fatText.trim().toDoubleOrNull()
    val weightValid = weight == null || weight in 20.0..300.0
    val fatValid = fat == null || fat in 1.0..70.0
    val canSave = (weight != null || fat != null) && weightValid && fatValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "新增身體數據" else "編輯身體數據")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "日期",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatEpochDay(epochDay))
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("體重（kg）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = !weightValid
                )
                OutlinedTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = { Text("體脂肪（%）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = !fatValid
                )
                if (!weightValid || !fatValid) {
                    Text(
                        "體重需在 20–300 kg，體脂肪需在 1–70%。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        epochDay,
                        weight?.takeIf { weightValid },
                        fat?.takeIf { fatValid }
                    )
                },
                enabled = canSave
            ) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = epochDay * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            epochDay = millis / 86_400_000L
                        }
                        showDatePicker = false
                    }
                ) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun formatBodyFat(value: Double): String =
    String.format(Locale.US, "%.1f%%", value)

private fun formatBodyFatNumber(value: Double): String =
    String.format(Locale.US, "%.1f", value)

private fun formatChartValue(value: Double, type: BodyMetricType): String =
    when (type) {
        BodyMetricType.WEIGHT -> "${formatWeight(value)} kg"
        BodyMetricType.FAT -> formatBodyFat(value)
    }

private fun chartDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("M/d"))
