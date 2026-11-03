package com.komugi.workout.ui.log

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.EntryEntity
import com.komugi.workout.data.EntryUi
import com.komugi.workout.data.SessionSummary
import com.komugi.workout.data.SetEntity
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.components.MuscleMap
import com.komugi.workout.ui.components.muscleGroupsIn
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.TargetParser
import com.komugi.workout.util.formatEpochDay
import com.komugi.workout.util.formatWeight
import com.komugi.workout.util.openUrl
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(
    repository: WorkoutRepository,
    onOpenSession: (Long) -> Unit
) {
    val summaries by repository.observeSummaries().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedEpochDay by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    val daysWithRecords = remember(summaries) {
        summaries.map { it.session.dateEpochDay }.toSet()
    }
    val daySessions = summaries.filter { it.session.dateEpochDay == selectedEpochDay }
    var pendingDelete by remember { mutableStateOf<SessionSummary?>(null) }
    var copySource by remember { mutableStateOf<SessionSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("紀錄") },
                colors = komugiTopAppBarColors(),
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val id = repository.createEmptySession(selectedEpochDay)
                                onOpenSession(id)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalContentColor.current
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("新紀錄")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthCalendar(
                selectedEpochDay = selectedEpochDay,
                epochDaysWithRecords = daysWithRecords,
                onSelect = { selectedEpochDay = it }
            )
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatEpochDay(selectedEpochDay),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${daySessions.size} 筆紀錄") }
                )
            }
            if (daySessions.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("這天還沒有紀錄", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "點右上「新紀錄」新增這天的訓練",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(daySessions, key = { it.session.id }) { summary ->
                        Card(
                            onClick = { onOpenSession(summary.session.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        summary.session.menuName ?: "自由訓練",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${summary.entryCount} 個動作",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { copySource = summary }) {
                                    Text("複製")
                                }
                                IconButton(onClick = { pendingDelete = summary }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "刪除這筆")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { summary ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除這筆紀錄？") },
            text = {
                Text("「${summary.session.menuName ?: "自由訓練"}」的所有動作與組數都會刪除，無法復原。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { repository.deleteSession(summary.session) }
                        pendingDelete = null
                    }
                ) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    copySource?.let { source ->
        SessionCopyDatePicker(
            initialEpochDay = LocalDate.now().toEpochDay(),
            onDismiss = { copySource = null },
            onConfirm = { targetDay ->
                scope.launch {
                    repository.copySessionToDate(source.session.id, targetDay)
                    Toast.makeText(
                        context,
                        "已複製到 ${formatEpochDay(targetDay)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                copySource = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    repository: WorkoutRepository,
    sessionId: Long,
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
    onOpenSummary: () -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    onOpenAddExercises: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ui by repository.observeSessionUi(sessionId).collectAsState(initial = null)

    var showDatePicker by remember { mutableStateOf(false) }
    var showCopyPicker by remember { mutableStateOf(false) }
    var showDeleteSession by remember { mutableStateOf(false) }
    var sheetEntry by remember { mutableStateOf<EntryUi?>(null) }
    var dialogEntryUi by remember { mutableStateOf<EntryUi?>(null) }
    var dialogType by remember { mutableStateOf<RecordType?>(null) }
    var editingSet by remember { mutableStateOf<SetEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui?.session?.menuName ?: "訓練紀錄") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onOpenSummary,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalContentColor.current
                        )
                    ) {
                        Text("總結")
                    }
                    TextButton(
                        onClick = { showCopyPicker = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalContentColor.current
                        )
                    ) {
                        Text("複製")
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "修改日期")
                    }
                    IconButton(onClick = { showDeleteSession = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "刪除整筆紀錄")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddExercises,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新增動作") }
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        formatEpochDay(data.session.dateEpochDay),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "日期預設當天，可點右上日曆修改；右上垃圾桶可刪除整筆紀錄",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (data.entries.isEmpty()) {
                    item {
                        Text(
                            "尚無動作，點右下「新增動作」開始。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(data.entries, key = { it.entry.id }) { entryUi ->
                    EntryCard(
                        entryUi = entryUi,
                        onAddRecord = { sheetEntry = entryUi },
                        onDuplicateSet = { scope.launch { repository.duplicateLastSet(entryUi.entry.id) } },
                        onDeleteEntry = { scope.launch { repository.deleteEntry(entryUi.entry) } },
                        onDeleteSet = { set -> scope.launch { repository.deleteSet(set) } },
                        onEditSet = { set -> editingSet = set },
                        onOpenHistory = { onOpenExerciseHistory(entryUi.entry.id) },
                        onOpenGuide = {
                            entryUi.entry.guideAsset?.let { guideId -> onOpenGuide(guideId) }
                        },
                        onOpenVideo = {
                            entryUi.entry.videoUrl?.let { url -> openUrl(context, url) }
                        }
                    )
                }
            }
        }
    }

    val session = ui?.session
    if (showDatePicker && session != null) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = session.dateEpochDay * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            scope.launch {
                                repository.updateDate(session, millis / 86_400_000L)
                            }
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

    if (showDeleteSession && session != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSession = false },
            title = { Text("刪除整筆紀錄？") },
            text = { Text("「${formatEpochDay(session.dateEpochDay)}」的所有動作與組數都會刪除，無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSession = false
                        scope.launch {
                            repository.deleteSession(session)
                            onBack()
                        }
                    }
                ) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSession = false }) { Text("取消") }
            }
        )
    }

    if (showCopyPicker && session != null) {
        SessionCopyDatePicker(
            initialEpochDay = LocalDate.now().toEpochDay(),
            onDismiss = { showCopyPicker = false },
            onConfirm = { targetDay ->
                showCopyPicker = false
                scope.launch {
                    repository.copySessionToDate(session.id, targetDay)
                    Toast.makeText(
                        context,
                        "已複製到 ${formatEpochDay(targetDay)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    sheetEntry?.let { entryUi ->
        AddRecordSheet(
            entryName = entryUi.entry.name,
            onDismiss = { sheetEntry = null },
            onPick = { type ->
                dialogEntryUi = entryUi
                dialogType = type
                sheetEntry = null
            }
        )
    }

    val activeEntryUi = dialogEntryUi
    val activeType = dialogType
    if (activeEntryUi != null && activeType != null) {
        RecordInputDialog(
            type = activeType,
            entryName = activeEntryUi.entry.name,
            prefill = buildPrefill(activeEntryUi, activeType),
            onDismiss = {
                dialogEntryUi = null
                dialogType = null
            },
            onConfirm = { value1, value2, bodyweight ->
                applyRecord(
                    repository = repository,
                    scope = scope,
                    entry = activeEntryUi.entry,
                    type = activeType,
                    value1 = value1,
                    value2 = value2,
                    bodyweight = bodyweight
                )
                dialogEntryUi = null
                dialogType = null
            }
        )
    }

    editingSet?.let { set ->
        SetEditDialog(
            set = set,
            onDismiss = { editingSet = null },
            onConfirm = { weight, reps, duration, bodyweight ->
                scope.launch {
                    repository.updateSet(
                        set = set,
                        weight = weight,
                        reps = reps,
                        duration = duration,
                        bodyweight = bodyweight
                    )
                }
                editingSet = null
            }
        )
    }
}

private data class RecordPrefill(
    val weight: String = "",
    val reps: String = "",
    val time: String = "",
    val sets: String = "",
    val equipment: String = "",
    val bodyweight: Boolean = false
)

private fun buildPrefill(entryUi: EntryUi, type: RecordType): RecordPrefill {
    val entry = entryUi.entry
    val sets = entryUi.sets
    val last = sets.lastOrNull()

    val autoBodyweight = entry.equipment?.trim() == "自重"
    val parsedWeight = TargetParser.kgFromEquipment(entry.equipment)
    val parsedReps = TargetParser.firstNumberAtStart(entry.targetReps)?.toInt()
    val parsedTime = TargetParser.seconds(entry.targetReps)
    val parsedSets = TargetParser.firstNumberAtStart(entry.targetSets)?.toInt()

    val weight = last?.weightKg?.let { formatWeight(it) }
        ?: parsedWeight?.let { formatWeight(it) }
        ?: ""
    val reps = last?.reps?.toString() ?: parsedReps?.toString() ?: ""
    val time = last?.durationSec?.toString() ?: parsedTime?.toString() ?: ""
    val bodyweight = last?.isBodyweight ?: autoBodyweight

    return when (type) {
        RecordType.FULL_SET -> RecordPrefill(weight = weight, reps = reps, bodyweight = bodyweight)
        RecordType.WEIGHT -> RecordPrefill(weight = weight, bodyweight = bodyweight)
        RecordType.REPS -> RecordPrefill(reps = reps)
        RecordType.TIME -> RecordPrefill(time = time)
        RecordType.SETS -> RecordPrefill(
            sets = (sets.size.takeIf { it > 0 } ?: parsedSets)?.toString() ?: ""
        )
        RecordType.EQUIPMENT -> RecordPrefill(equipment = entry.equipment ?: "")
    }
}

private fun applyRecord(
    repository: WorkoutRepository,
    scope: CoroutineScope,
    entry: EntryEntity,
    type: RecordType,
    value1: String,
    value2: String,
    bodyweight: Boolean
) {
    when (type) {
        RecordType.WEIGHT -> {
            if (bodyweight) {
                scope.launch { repository.addSet(entry.id, bodyweight = true) }
            } else {
                value1.toDoubleOrNull()?.let { value ->
                    scope.launch { repository.addSet(entry.id, weight = value) }
                }
            }
        }
        RecordType.REPS -> value1.toIntOrNull()?.let { value ->
            scope.launch { repository.addSet(entry.id, reps = value) }
        }
        RecordType.SETS -> value1.toIntOrNull()?.takeIf { it >= 0 }?.let { value ->
            scope.launch { repository.setTotalSets(entry.id, value) }
        }
        RecordType.TIME -> value1.toIntOrNull()?.let { value ->
            scope.launch { repository.addSet(entry.id, duration = value) }
        }
        RecordType.EQUIPMENT -> if (value1.isNotBlank()) {
            scope.launch { repository.updateEntry(entry.copy(equipment = value1.trim())) }
        }
        RecordType.FULL_SET -> {
            scope.launch {
                repository.addSet(
                    entryId = entry.id,
                    weight = if (bodyweight) null else value1.toDoubleOrNull(),
                    reps = value2.toIntOrNull(),
                    bodyweight = bodyweight
                )
            }
        }
    }
}

@Composable
private fun EntryCard(
    entryUi: EntryUi,
    onAddRecord: () -> Unit,
    onDuplicateSet: () -> Unit,
    onDeleteEntry: () -> Unit,
    onDeleteSet: (SetEntity) -> Unit,
    onEditSet: (SetEntity) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenVideo: () -> Unit
) {
    val sets = entryUi.sets
    val entry = entryUi.entry
    val totalReps = sets.sumOf { it.reps ?: 0 }
    val maxWeight = sets.mapNotNull { it.weightKg }.maxOrNull()
    val totalTime = sets.sumOf { it.durationSec ?: 0 }

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        if (entry.supersetKey != null) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Spacer(Modifier.width(6.dp))
        }
        Card(Modifier.weight(1f)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                    entry.supersetKey?.let {
                        Text(
                            "超級組 $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    val target = buildString {
                        entry.targetSets?.let { append("$it 組") }
                        entry.targetReps?.let {
                            if (isNotEmpty()) append(" × ")
                            append(it)
                        }
                        entry.restSec?.let {
                            if (isNotEmpty()) append(" · ")
                            append("休 $it")
                        }
                    }
                    if (target.isNotEmpty()) {
                        Text(
                            "建議：$target",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    entry.equipment?.let {
                        Text(
                            "器材：$it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    entry.targetParts?.let {
                        Text(
                            "部位：$it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onDeleteEntry) {
                    Icon(Icons.Filled.Delete, contentDescription = "刪除動作")
                }
            }

            val groups = muscleGroupsIn(entry.targetParts, limit = 3)
            if (groups.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        MuscleMap(
                            group = group,
                            modifier = Modifier.size(42.dp, 56.dp)
                        )
                    }
                }
            }

            if (sets.isNotEmpty()) {
                val bodyweightCount = sets.count { it.isBodyweight }
                val parts = buildList {
                    add("${sets.size} 組")
                    if (totalReps > 0) add("$totalReps 下")
                    maxWeight?.let { add("最高 ${formatWeight(it)} kg") }
                    if (bodyweightCount > 0) add("自重 $bodyweightCount 組")
                    if (totalTime > 0) add("$totalTime 秒")
                }
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    "尚未記錄",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (sets.isEmpty()) {
                Text(
                    "每一列代表一組；按「新增紀錄」新增一列",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SetsHeader()
                sets.forEachIndexed { index, set ->
                    SetRow(
                        index = index,
                        set = set,
                        onEdit = { onEditSet(set) },
                        onDelete = { onDeleteSet(set) }
                    )
                }
                Text(
                    "點任一列可編輯該組數值",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAddRecord) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("新增紀錄")
                }
                TextButton(onClick = onDuplicateSet) { Text("＋組") }
                TextButton(onClick = onOpenHistory) { Text("歷史") }
                if (entry.guideAsset != null) {
                    TextButton(onClick = onOpenGuide) { Text("說明") }
                }
                if (entry.videoUrl != null) {
                    IconButton(onClick = onOpenVideo) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "影片")
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SetsHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "組",
            Modifier.weight(0.6f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "重量",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "次數",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "時間",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: SetEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index + 1}", Modifier.weight(0.6f))
        Text(
            when {
                set.isBodyweight -> "自重"
                set.weightKg != null -> "${formatWeight(set.weightKg)} kg"
                else -> "—"
            },
            Modifier.weight(1f)
        )
        Text(set.reps?.toString() ?: "—", Modifier.weight(1f))
        Text(set.durationSec?.let { "${it}s" } ?: "—", Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "刪除組",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

enum class RecordType(val label: String, val hint: String) {
    FULL_SET("完整一組（重量＋次數）", "新增一列，最常用"),
    WEIGHT("重量", "新增一列，只填重量"),
    REPS("次數", "新增一列，只填次數"),
    SETS("組數", "調整總組數"),
    TIME("時間", "新增一列，填秒數"),
    EQUIPMENT("器材", "修改本動作的器材")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordSheet(
    entryName: String,
    onDismiss: () -> Unit,
    onPick: (RecordType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "新增紀錄 · $entryName",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Text(
                "每個選項都會新增獨立的一列（點列可再編輯）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            RecordType.entries.forEach { type ->
                ListItem(
                    headlineContent = { Text(type.label) },
                    supportingContent = { Text(type.hint) },
                    modifier = Modifier.clickable { onPick(type) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecordInputDialog(
    type: RecordType,
    entryName: String,
    prefill: RecordPrefill,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var value1 by remember {
        mutableStateOf(
            when (type) {
                RecordType.FULL_SET, RecordType.WEIGHT -> prefill.weight
                RecordType.REPS -> prefill.reps
                RecordType.TIME -> prefill.time
                RecordType.SETS -> prefill.sets
                RecordType.EQUIPMENT -> prefill.equipment
            }
        )
    }
    var value2 by remember { mutableStateOf(prefill.reps) }
    var bodyweight by remember { mutableStateOf(prefill.bodyweight) }
    val supportsBodyweight = type == RecordType.FULL_SET || type == RecordType.WEIGHT

    val title = when (type) {
        RecordType.FULL_SET -> "完整一組"
        RecordType.WEIGHT -> "重量（kg）"
        RecordType.REPS -> "次數"
        RecordType.SETS -> "組數"
        RecordType.TIME -> "時間（秒）"
        RecordType.EQUIPMENT -> "器材"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title · $entryName") },
        text = {
            when (type) {
                RecordType.EQUIPMENT -> {
                    Column {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("啞鈴", "壺鈴", "彈力帶", "TRX", "槓片", "自重").forEach { item ->
                                AssistChip(
                                    onClick = { value1 = item },
                                    label = { Text(item) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = value1,
                            onValueChange = { value1 = it },
                            label = { Text("器材名稱") },
                            singleLine = true
                        )
                    }
                }
                RecordType.FULL_SET -> {
                    Column {
                        BodyweightToggle(bodyweight) { bodyweight = it }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = if (bodyweight) "" else value1,
                                onValueChange = { value1 = it },
                                label = { Text("重量 kg") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                enabled = !bodyweight,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = value2,
                                onValueChange = { value2 = it },
                                label = { Text("次數") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                RecordType.WEIGHT -> {
                    Column {
                        BodyweightToggle(bodyweight) { bodyweight = it }
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = if (bodyweight) "" else value1,
                            onValueChange = { value1 = it },
                            label = { Text("重量（kg）") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !bodyweight
                        )
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = value1,
                        onValueChange = { value1 = it },
                        label = {
                            Text(
                                when (type) {
                                    RecordType.REPS -> "次數"
                                    RecordType.SETS -> "組數"
                                    RecordType.TIME -> "秒數"
                                    else -> "數值"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value1, value2, bodyweight) },
                enabled = bodyweight || value1.isNotBlank() || value2.isNotBlank()
            ) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun BodyweightToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onChange(!checked) }
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text("自重（不計重量）", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SetEditDialog(
    set: SetEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double?, Int?, Int?, Boolean) -> Unit
) {
    var weight by remember {
        mutableStateOf(if (set.isBodyweight) "" else set.weightKg?.let { formatWeight(it) } ?: "")
    }
    var reps by remember { mutableStateOf(set.reps?.toString() ?: "") }
    var duration by remember { mutableStateOf(set.durationSec?.toString() ?: "") }
    var bodyweight by remember { mutableStateOf(set.isBodyweight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯第 ${set.setIndex} 組") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyweightToggle(bodyweight) { bodyweight = it }
                OutlinedTextField(
                    value = if (bodyweight) "" else weight,
                    onValueChange = { weight = it },
                    label = { Text("重量（kg）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !bodyweight
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("次數") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("時間（秒）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        if (bodyweight) null else weight.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                        reps.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                        duration.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                        bodyweight
                    )
                }
            ) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCopyDatePicker(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay * 86_400_000L
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(millis / 86_400_000L)
                    } ?: onDismiss()
                }
            ) { Text("複製到這天") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = state)
    }
}

