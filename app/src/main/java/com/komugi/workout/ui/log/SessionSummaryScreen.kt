package com.komugi.workout.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.EntryEntity
import com.komugi.workout.data.SessionUi
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.components.MuscleGroup
import com.komugi.workout.ui.components.MuscleMap
import com.komugi.workout.ui.components.muscleGroupLabel
import com.komugi.workout.ui.components.primaryMuscleGroup
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.TargetParser
import com.komugi.workout.util.formatEpochDay
import com.komugi.workout.util.formatWeight
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private data class EntryProgress(
    val entry: EntryEntity,
    val completedSets: Int,
    val targetSets: Int,
    val totalReps: Int,
    val totalDuration: Int,
    val volume: Double,
    val maxWeight: Double?,
    val bodyweightSets: Int
) {
    val completion: Float =
        if (targetSets <= 0) 0f else (completedSets.toFloat() / targetSets).coerceIn(0f, 1f)
    val isComplete: Boolean = completedSets >= targetSets
}

private data class SessionStats(
    val entries: List<EntryProgress>,
    val startedEntries: Int,
    val totalEntries: Int,
    val completedSets: Int,
    val targetSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val totalDuration: Int,
    val completion: Float,
    val muscleSets: List<Pair<MuscleGroup, Int>>
)

private fun computeStats(ui: SessionUi): SessionStats {
    val entries = ui.entries.map { entryUi ->
        val target = TargetParser.firstNumberAtStart(entryUi.entry.targetSets)
            ?.toInt()
            ?.takeIf { it > 0 }
            ?: entryUi.sets.size.coerceAtLeast(1)
        EntryProgress(
            entry = entryUi.entry,
            completedSets = entryUi.sets.size,
            targetSets = target,
            totalReps = entryUi.sets.sumOf { it.reps ?: 0 },
            totalDuration = entryUi.sets.sumOf { it.durationSec ?: 0 },
            volume = entryUi.sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
            maxWeight = entryUi.sets.mapNotNull { it.weightKg }.maxOrNull(),
            bodyweightSets = entryUi.sets.count { it.isBodyweight }
        )
    }
    val targetSum = entries.sumOf { it.targetSets }
    val completedSum = entries.sumOf { it.completedSets }
    val cappedSum = entries.sumOf { minOf(it.completedSets, it.targetSets) }
    val completion = if (targetSum <= 0) 0f else cappedSum.toFloat() / targetSum
    val muscleSets = entries
        .filter { it.completedSets > 0 }
        .mapNotNull { ep ->
            primaryMuscleGroup(ep.entry.targetParts)?.let { group -> group to ep.completedSets }
        }
        .groupBy({ it.first }, { it.second })
        .map { (group, sets) -> group to sets.sum() }
        .sortedByDescending { it.second }

    return SessionStats(
        entries = entries,
        startedEntries = entries.count { it.completedSets > 0 },
        totalEntries = entries.size,
        completedSets = completedSum,
        targetSets = targetSum,
        totalReps = entries.sumOf { it.totalReps },
        totalVolume = entries.sumOf { it.volume },
        totalDuration = entries.sumOf { it.totalDuration },
        completion = completion,
        muscleSets = muscleSets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    repository: WorkoutRepository,
    sessionId: Long,
    onBack: () -> Unit,
    onOpenExerciseHistory: (Long) -> Unit
) {
    val ui by repository.observeSessionUi(sessionId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("訓練總結") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val data = ui
        if (data == null) {
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

        val stats = remember(data) { computeStats(data) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SummaryHero(data = data, stats = stats) }
            item { SummaryStatsCard(stats = stats) }
            if (stats.muscleSets.isNotEmpty()) {
                item { MuscleBalanceCard(stats = stats) }
            }
            item {
                Text(
                    "動作完成度",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (stats.entries.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("這筆紀錄還沒有動作", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "回到紀錄頁新增動作與組數，這裡就會出現完成度與訓練量。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(stats.entries, key = { it.entry.id }) { entry ->
                    EntryProgressCard(
                        entry = entry,
                        onClick = { onOpenExerciseHistory(entry.entry.id) }
                    )
                }
            }
            item {
                Text(
                    "完成度以建議組數計算；沒有建議組數的動作，以已記錄的組數視為完成。" +
                        "訓練量＝重量 × 次數，自重組不計入。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryHero(data: SessionUi, stats: SessionStats) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { stats.completion },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 9.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(stats.completion * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "完成度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    data.session.menuName ?: "自由訓練",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    formatEpochDay(data.session.dateEpochDay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    summaryMessage(stats),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stats.startedEntries} / ${stats.totalEntries} 個動作有紀錄",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStatsCard(stats: SessionStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row {
                StatCell(
                    label = "動作",
                    value = "${stats.startedEntries}/${stats.totalEntries}",
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = if (stats.targetSets > 0) "組數（目標 ${stats.targetSets}）" else "組數",
                    value = "${stats.completedSets}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                StatCell(
                    label = "總次數",
                    value = "${stats.totalReps} 下",
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "訓練量",
                    value = if (stats.totalVolume > 0) {
                        "${formatVolume(stats.totalVolume)} kg"
                    } else {
                        "—"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (stats.entries.any { it.completedSets > it.targetSets }) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "超額組數會保留在紀錄中，但不計入完成度。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (stats.totalDuration > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "累計時間 ${formatDuration(stats.totalDuration)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun MuscleBalanceCard(stats: SessionStats) {
    val total = stats.muscleSets.sumOf { it.second }.coerceAtLeast(1)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("訓練部位分佈", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "依主要訓練部位與已完成組數統計",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            stats.muscleSets.take(5).forEachIndexed { index, (group, sets) ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                val ratio = sets.toFloat() / total
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MuscleMap(
                        group = group,
                        modifier = Modifier.size(40.dp, 52.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                muscleGroupLabel(group),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(ratio * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "$sets 組",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryProgressCard(entry: EntryProgress, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(entry.entry.name, style = MaterialTheme.typography.titleMedium)
                    val target = buildString {
                        entry.entry.targetSets?.let { append("$it 組") }
                        entry.entry.targetReps?.let {
                            if (isNotEmpty()) append(" × ")
                            append(it)
                        }
                        entry.entry.restSec?.let {
                            if (isNotEmpty()) append(" · ")
                            append("休 $it")
                        }
                    }
                    Text(
                        if (target.isNotBlank()) "建議：$target" else "自訂動作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(entry.completion * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (entry.isComplete) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { entry.completion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val detail = buildList {
                if (entry.completedSets > entry.targetSets) {
                    add("${entry.completedSets} 組（目標 ${entry.targetSets}）")
                } else {
                    add("${entry.completedSets}/${entry.targetSets} 組")
                }
                if (entry.totalReps > 0) add("${entry.totalReps} 下")
                entry.maxWeight?.let { add("最高 ${formatWeight(it)} kg") }
                if (entry.bodyweightSets > 0) add("自重 ${entry.bodyweightSets} 組")
                if (entry.totalDuration > 0) add(formatDuration(entry.totalDuration))
            }
            Text(
                detail.joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "查看歷史 ›",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}

private fun summaryMessage(stats: SessionStats): String = when {
    stats.completedSets == 0 -> "還沒有記錄組數，先從紀錄頁開始。"
    stats.completion >= 0.999f -> "全部完成，今天很扎實！"
    stats.completion >= 0.8f -> "幾乎完成了，再補幾組就達標。"
    stats.completion >= 0.5f -> "已經完成一半，保持節奏。"
    else -> "有開始就是進步，下次再多一組。"
}

private fun formatDuration(seconds: Int): String =
    if (seconds >= 60) {
        "${seconds / 60} 分 ${seconds % 60} 秒"
    } else {
        "$seconds 秒"
    }

private fun formatVolume(value: Double): String =
    String.format(Locale.US, "%,d", value.roundToLong())
