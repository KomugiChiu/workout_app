package com.komugi.workout.ui.log

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.NewEntryInput
import com.komugi.workout.data.NewMenuExerciseInput
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.components.MuscleGroup
import com.komugi.workout.ui.components.muscleGroupLabel
import com.komugi.workout.ui.components.muscleGroupsIn
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import kotlinx.coroutines.launch

private enum class AddStep {
    PICK,
    CONFIGURE
}

private data class ExerciseDraft(
    val exercise: ExerciseEntity,
    val sets: String = "3",
    val reps: String = "10",
    val rest: String = "60",
    val superset: String? = null
)

private val EquipmentCategories = listOf(
    "自重", "啞鈴", "壺鈴", "槓鈴", "槓片", "彈力帶", "TRX", "其他"
)

private fun equipmentCategory(raw: String?): String {
    val text = raw?.trim().orEmpty()
    return when {
        text.isEmpty() -> "其他"
        text.contains("自重") -> "自重"
        text.contains("壺鈴") || text.contains("ViPR", ignoreCase = true) -> "壺鈴"
        text.contains("TRX") || text.contains("懸吊") -> "TRX"
        text.contains("彈力帶") || text.contains("帶") -> "彈力帶"
        text.contains("槓片") -> "槓片"
        text.contains("槓") -> "槓鈴"
        text.contains("啞鈴") -> "啞鈴"
        Regex("\\d+\\s*kg", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "啞鈴"
        else -> "其他"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExercisesScreen(
    repository: WorkoutRepository,
    sessionId: Long,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val exercises by repository.observeExercises().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var step by remember { mutableStateOf(AddStep.PICK) }
    var search by remember { mutableStateOf("") }
    var partFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var equipmentFilter by remember { mutableStateOf<String?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showSaveMenuDialog by remember { mutableStateOf(false) }

    val selectedIds = remember { mutableStateListOf<Long>() }
    val drafts = remember { mutableStateListOf<ExerciseDraft>() }

    val filtered = remember(exercises, search, partFilter, equipmentFilter) {
        val keyword = search.trim()
        exercises.filter { exercise ->
            (keyword.isBlank() || exercise.name.contains(keyword, ignoreCase = true)) &&
                (partFilter == null ||
                    muscleGroupsIn(exercise.parts, limit = 10).contains(partFilter)) &&
                (equipmentFilter == null || equipmentCategory(exercise.equipment) == equipmentFilter)
        }
    }

    fun toggle(id: Long) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
    }

    fun proceed() {
        val previous = drafts.associateBy { it.exercise.id }
        val selected = exercises.filter { it.id in selectedIds }
        drafts.clear()
        selected.forEach { exercise ->
            drafts.add(previous[exercise.id] ?: ExerciseDraft(exercise))
        }
        step = AddStep.CONFIGURE
    }

    fun confirm() {
        if (drafts.isEmpty()) return
        val counts = drafts.mapNotNull { it.superset }.groupingBy { it }.eachCount()
        val items = drafts.map { draft ->
            NewEntryInput(
                name = draft.exercise.name,
                equipment = draft.exercise.equipment,
                targetParts = draft.exercise.parts,
                exerciseId = draft.exercise.id,
                targetSets = draft.sets.trim().ifBlank { null },
                targetReps = draft.reps.trim().ifBlank { null },
                restSec = draft.rest.trim().ifBlank { null },
                supersetKey = draft.superset?.takeIf { (counts[it] ?: 0) >= 2 }
            )
        }
        scope.launch {
            repository.addEntries(sessionId, items)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (step == AddStep.PICK) "新增動作" else "設定組次") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step == AddStep.CONFIGURE) step = AddStep.PICK else onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (step == AddStep.PICK) {
                        TextButton(
                            onClick = { showCustomDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = LocalContentColor.current
                            )
                        ) {
                            Text("自訂動作")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (step == AddStep.PICK) {
                            "已選 ${selectedIds.size} 個"
                        } else {
                            "${drafts.size} 個動作"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (step == AddStep.CONFIGURE) {
                        OutlinedButton(
                            onClick = { showSaveMenuDialog = true },
                            enabled = drafts.isNotEmpty()
                        ) {
                            Text("存為菜單")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            if (step == AddStep.PICK) proceed() else confirm()
                        },
                        enabled = if (step == AddStep.PICK) {
                            selectedIds.isNotEmpty()
                        } else {
                            drafts.isNotEmpty()
                        }
                    ) {
                        Text(
                            if (step == AddStep.PICK) {
                                "下一步（${selectedIds.size}）"
                            } else {
                                "加入訓練（${drafts.size}）"
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (step == AddStep.PICK) {
            ExercisePicker(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                exercises = filtered,
                totalCount = exercises.size,
                search = search,
                onSearchChange = { search = it },
                partFilter = partFilter,
                onPartFilterChange = { partFilter = it },
                equipmentFilter = equipmentFilter,
                onEquipmentFilterChange = { equipmentFilter = it },
                selectedIds = selectedIds,
                onToggle = ::toggle,
                onClearFilters = {
                    search = ""
                    partFilter = null
                    equipmentFilter = null
                }
            )
        } else {
            ExerciseConfiguration(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                drafts = drafts,
                onChange = { index, draft -> drafts[index] = draft },
                onRemove = { index -> drafts.removeAt(index) }
            )
        }
    }

    if (showCustomDialog) {
        CustomExerciseDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, parts, equipment ->
                scope.launch {
                    repository.rememberExercise(name, parts, equipment)
                    repository.exerciseByName(name)?.let { exercise ->
                        if (!selectedIds.contains(exercise.id)) selectedIds.add(exercise.id)
                    }
                    showCustomDialog = false
                }
            }
        )
    }

    if (showSaveMenuDialog) {
        SaveMenuDialog(
            onDismiss = { showSaveMenuDialog = false },
            onConfirm = { name, alsoAdd ->
                val counts = drafts.mapNotNull { it.superset }
                    .groupingBy { it }
                    .eachCount()
                val normalized = drafts.map { draft ->
                    draft.copy(
                        superset = draft.superset?.takeIf { (counts[it] ?: 0) >= 2 }
                    )
                }
                val menuItems = normalized.map {
                    NewMenuExerciseInput(
                        exerciseId = it.exercise.id,
                        equipment = it.exercise.equipment,
                        targetSets = it.sets.trim().ifBlank { null },
                        targetReps = it.reps.trim().ifBlank { null },
                        restSec = it.rest.trim().ifBlank { null },
                        supersetKey = it.superset
                    )
                }
                showSaveMenuDialog = false
                scope.launch {
                    repository.createCustomMenu(name, null, menuItems)
                    if (alsoAdd) {
                        repository.addEntries(
                            sessionId,
                            normalized.map {
                                NewEntryInput(
                                    name = it.exercise.name,
                                    equipment = it.exercise.equipment,
                                    targetParts = it.exercise.parts,
                                    exerciseId = it.exercise.id,
                                    targetSets = it.sets.trim().ifBlank { null },
                                    targetReps = it.reps.trim().ifBlank { null },
                                    restSec = it.rest.trim().ifBlank { null },
                                    supersetKey = it.superset
                                )
                            }
                        )
                    }
                    Toast.makeText(
                        context,
                        if (alsoAdd) "已存為菜單並加入訓練" else "已存為自訂菜單",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDone()
                }
            }
        )
    }
}

@Composable
private fun ExercisePicker(
    modifier: Modifier,
    exercises: List<ExerciseEntity>,
    totalCount: Int,
    search: String,
    onSearchChange: (String) -> Unit,
    partFilter: MuscleGroup?,
    onPartFilterChange: (MuscleGroup?) -> Unit,
    equipmentFilter: String?,
    onEquipmentFilterChange: (String?) -> Unit,
    selectedIds: List<Long>,
    onToggle: (Long) -> Unit,
    onClearFilters: () -> Unit
) {
    val hasFilter = search.isNotBlank() || partFilter != null || equipmentFilter != null
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜尋動作") },
                singleLine = true
            )
        }
        item {
            FilterChipRow(
                title = "部位",
                options = listOf<MuscleGroup?>(null) + MuscleGroup.entries,
                selected = partFilter,
                label = { group -> group?.let { muscleGroupLabel(it) } ?: "全部" },
                onSelect = onPartFilterChange
            )
        }
        item {
            FilterChipRow(
                title = "器材",
                options = listOf<String?>(null) + EquipmentCategories,
                selected = equipmentFilter,
                label = { item -> item ?: "全部" },
                onSelect = onEquipmentFilterChange
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "共 ${exercises.size} / $totalCount 個動作",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (hasFilter) {
                    TextButton(onClick = onClearFilters) { Text("清除篩選") }
                }
            }
        }
        if (exercises.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("沒有符合的動作", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "試試其他關鍵字，或放寬部位／器材篩選。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(exercises, key = { it.id }) { exercise ->
                ExercisePickRow(
                    exercise = exercise,
                    checked = exercise.id in selectedIds,
                    onToggle = { onToggle(exercise.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FilterChipRow(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

@Composable
private fun ExercisePickRow(
    exercise: ExerciseEntity,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                if (exercise.isCustom) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "自訂",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val meta = listOfNotNull(exercise.parts, exercise.equipment).joinToString(" · ")
            Text(
                if (meta.isBlank()) "未標記部位／器材" else meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun ExerciseConfiguration(
    modifier: Modifier,
    drafts: List<ExerciseDraft>,
    onChange: (Int, ExerciseDraft) -> Unit,
    onRemove: (Int) -> Unit
) {
    val counts = drafts.mapNotNull { it.superset }.groupingBy { it }.eachCount()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "設定每個動作的建議組數、次數與休息。超級組用 A／B／C 標記，同一組至少兩個動作；只有一個的會自動當作單獨。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (counts.isNotEmpty()) {
            item {
                Text(
                    "超級組：" + counts.entries.joinToString(" · ") { "${it.key} ${it.value} 個" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        if (drafts.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("尚未選擇動作", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "按左上返回選擇動作。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(drafts, key = { _, draft -> draft.exercise.id }) { index, draft ->
                DraftCard(
                    draft = draft,
                    onChange = { onChange(index, it) },
                    onRemove = { onRemove(index) }
                )
            }
        }
    }
}

@Composable
private fun DraftCard(
    draft: ExerciseDraft,
    onChange: (ExerciseDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(draft.exercise.name, style = MaterialTheme.typography.titleMedium)
                    val meta = listOfNotNull(draft.exercise.parts, draft.exercise.equipment)
                        .joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "移除")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.sets,
                    onValueChange = { onChange(draft.copy(sets = it)) },
                    label = { Text("組數") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.reps,
                    onValueChange = { onChange(draft.copy(reps = it)) },
                    label = { Text("次數／秒") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.rest,
                    onValueChange = { onChange(draft.copy(rest = it)) },
                    label = { Text("休息秒") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "超級組",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = draft.superset == null,
                    onClick = { onChange(draft.copy(superset = null)) },
                    label = { Text("單獨") }
                )
                listOf("A", "B", "C").forEach { key ->
                    FilterChip(
                        selected = draft.superset == key,
                        onClick = { onChange(draft.copy(superset = key)) },
                        label = { Text(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增自訂動作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("動作名稱") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = parts,
                    onValueChange = { parts = it },
                    label = { Text("訓練部位（例：胸大肌、前三角）") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("器材（例：啞鈴）") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim(),
                        parts.trim().ifBlank { null },
                        equipment.trim().ifBlank { null }
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("建立並選取") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SaveMenuDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("我的課表") }
    var alsoAdd by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存為自訂菜單") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("菜單名稱") },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { alsoAdd = !alsoAdd }
                ) {
                    Checkbox(checked = alsoAdd, onCheckedChange = { alsoAdd = it })
                    Text("同時加入本次訓練")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), alsoAdd) },
                enabled = name.isNotBlank()
            ) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
