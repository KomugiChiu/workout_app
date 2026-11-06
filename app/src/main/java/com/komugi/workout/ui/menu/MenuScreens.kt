package com.komugi.workout.ui.menu

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.MenuExerciseRow
import com.komugi.workout.data.MenuSummary
import com.komugi.workout.data.NewMenuExerciseInput
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.data.resolveExerciseOverrides
import com.komugi.workout.ui.components.ExerciseMeta
import com.komugi.workout.ui.components.ExerciseRole
import com.komugi.workout.ui.components.SlotRecommendations
import com.komugi.workout.ui.components.SlotRequirement
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.TargetParser
import com.komugi.workout.util.openUrl
import java.time.LocalDate
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuListScreen(
    repository: WorkoutRepository,
    onOpenMenu: (String) -> Unit
) {
    val menus by repository.observeMenuSummaries().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("菜單") }, colors = komugiTopAppBarColors())
        }
    ) { padding ->
        if (menus.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "載入課表…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val templates = menus.filter { it.menu.isTemplate }
            val others = menus.filterNot { it.menu.isTemplate }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "選一份課表開始今天的紀錄；訓練計畫可依今天器材更換動作。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (templates.isNotEmpty()) {
                    item { MenuSectionHeader("訓練計畫") }
                    items(templates, key = { it.menu.id }) { summary ->
                        MenuCard(
                            summary = summary,
                            template = true,
                            onOpenMenu = onOpenMenu
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item { MenuSectionHeader("其他菜單") }
                    items(others, key = { it.menu.id }) { summary ->
                        MenuCard(
                            summary = summary,
                            template = false,
                            onOpenMenu = onOpenMenu
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MenuCard(
    summary: MenuSummary,
    template: Boolean,
    onOpenMenu: (String) -> Unit
) {
    Card(
        onClick = { onOpenMenu(summary.menu.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    summary.menu.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (summary.menu.isCustom) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "自訂",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (template) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "可調整",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            summary.menu.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onOpenMenu(summary.menu.id) },
                    label = { Text("${summary.exerciseCount} 個動作") }
                )
                if (template) {
                    AssistChip(
                        onClick = { onOpenMenu(summary.menu.id) },
                        label = { Text("依器材換動作") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDetailScreen(
    repository: WorkoutRepository,
    menuId: String?,
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onSessionCreated: (Long) -> Unit
) {
    val id = menuId ?: ""
    val menu by remember(id) { repository.observeMenu(id) }.collectAsState(initial = null)
    val rows by remember(id) {
        repository.observeMenuExercises(id)
    }.collectAsState(initial = emptyList())
    val exercises by repository.observeExercises().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var showDeleteMenu by remember { mutableStateOf(false) }
    var showSaveMenu by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showStartConfirm by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var swapTarget by remember { mutableStateOf<MenuExerciseRow?>(null) }
    var equipmentFilter by remember(id) { mutableStateOf<Set<String>>(emptySet()) }
    var equipmentLoaded by remember(id) { mutableStateOf(false) }
    var draftTouched by remember(id) { mutableStateOf(false) }
    var loaded by remember(id) { mutableStateOf(false) }
    var baselineRows by remember(id) {
        mutableStateOf<List<MenuExerciseRow>>(emptyList())
    }
    var autoPreview by remember { mutableStateOf<List<MenuExerciseRow>?>(null) }
    var autoChanges by remember {
        mutableStateOf<List<Triple<String, String, String>>>(emptyList())
    }
    var autoWarnings by remember { mutableStateOf<List<String>>(emptyList()) }
    val rowList = remember(id) { mutableStateListOf<MenuExerciseRow>() }
    val lazyListState = rememberLazyListState()

    val isAdjustable = rows.any { it.slotKey != null }

    LaunchedEffect(rows, exercises) {
        if (!loaded && rows.isNotEmpty() && exercises.isNotEmpty()) {
            val overrides = if (isAdjustable) {
                repository.lastUsedMenuExercises(id)
            } else {
                emptyList()
            }
            val applied = resolveExerciseOverrides(rows, overrides) { exerciseId ->
                exercises.firstOrNull { it.id == exerciseId }
            }
            rowList.clear()
            rowList.addAll(applied)
            baselineRows = applied
            loaded = true
        } else if (loaded && !draftTouched && !isAdjustable) {
            val currentIds = rowList.map { it.menuExerciseId }.toSet()
            val newIds = rows.map { it.menuExerciseId }.toSet()
            if (currentIds != newIds) {
                rowList.clear()
                rowList.addAll(rows)
            } else {
                rows.forEach { updated ->
                    val index = rowList.indexOfFirst {
                        it.menuExerciseId == updated.menuExerciseId
                    }
                    if (index >= 0) rowList[index] = updated
                }
            }
        }
    }

    LaunchedEffect(id) {
        if (!equipmentLoaded) {
            equipmentFilter = repository.equipmentProfile()
            equipmentLoaded = true
        }
    }

    LaunchedEffect(equipmentFilter, equipmentLoaded) {
        if (equipmentLoaded) {
            repository.saveEquipmentProfile(equipmentFilter)
        }
    }

    LaunchedEffect(rowList.toList(), rows, loaded, isAdjustable) {
        if (loaded && isAdjustable) {
            val current = rowList.toList()
            repository.saveLastUsedMenuExercises(
                id,
                if (current == rows) emptyList() else current,
                rows
            )
        }
    }

    val hasEdits = isAdjustable && loaded && rowList.toList() != baselineRows
    val hasOverrides = isAdjustable && loaded && baselineRows != rows
    val canReset = hasEdits || hasOverrides
    val differsFromTemplate = loaded && rowList.toList() != rows

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (!isAdjustable && from.index in rowList.indices && to.index in rowList.indices) {
            val moved = rowList.removeAt(from.index)
            rowList.add(to.index, moved)
            scope.launch {
                repository.reorderMenuExercises(
                    id,
                    rowList.map { it.menuExerciseId }
                )
            }
        }
    }

    fun startSession() {
        if (creating) return
        creating = true
        scope.launch {
            if (equipmentFilter.isNotEmpty()) {
                repository.saveEquipmentProfile(equipmentFilter)
            }
            val sessionId = repository.createSessionFromAdjustedMenu(
                menuId = id,
                epochDay = LocalDate.now().toEpochDay(),
                rows = rowList.toList(),
                equipmentNote = equipmentFilter.joinToString("、"),
                adjusted = hasEdits
            )
            if (sessionId > 0) {
                onSessionCreated(sessionId)
            } else {
                creating = false
                Toast.makeText(context, "建立紀錄失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun buildAutoAdjustment() {
        if (equipmentFilter.isEmpty()) {
            Toast.makeText(context, "請先選擇今天可用的器材", Toast.LENGTH_SHORT).show()
            return
        }
        val used = mutableSetOf<Long>()
        val changes = mutableListOf<Triple<String, String, String>>()
        val warnings = mutableListOf<String>()
        val newRows = rowList.map { row ->
            val current = exercises.firstOrNull { it.id == row.exerciseId }
            val currentCategory = ExerciseMeta.equipmentCategory(current?.equipment)
            if (currentCategory in equipmentFilter) {
                used.add(row.exerciseId)
                return@map row
            }
            val requirement = ExerciseRole.requirementOf(row, current)
            val recommendedNames = SlotRecommendations.namesFor(requirement)
            val candidate = exercises.asSequence()
                .filter { it.id != row.exerciseId && it.id !in used }
                .filter { ExerciseRole.isStrengthCandidate(it) }
                .filter { ExerciseRole.matchesRole(it, requirement.role) }
                .filter { ExerciseRole.lateralityMatches(it, requirement.laterality) }
                .filter { ExerciseMeta.equipmentCategory(it.equipment) in equipmentFilter }
                .sortedWith(
                    compareByDescending<ExerciseEntity> {
                        ExerciseRole.matchScore(it, requirement)
                    }
                        .thenBy {
                            recommendedNames.indexOf(it.name).let { index ->
                                if (index >= 0) index else Int.MAX_VALUE
                            }
                        }
                        .thenBy { it.name }
                )
                .firstOrNull()
            if (candidate == null) {
                used.add(row.exerciseId)
                warnings += "${row.slotKey ?: row.name}：找不到符合器材的替代動作，保留 ${row.name}"
                row
            } else {
                used.add(candidate.id)
                changes += Triple(row.slotKey ?: row.name, row.name, candidate.name)
                row.copy(
                    exerciseId = candidate.id,
                    name = candidate.name,
                    equipment = candidate.equipment,
                    parts = candidate.parts,
                    videoUrl = candidate.videoUrl,
                    guideId = candidate.guideId
                )
            }
        }
        if (changes.isEmpty()) {
            Toast.makeText(context, "目前器材不需要更換動作", Toast.LENGTH_SHORT).show()
            return
        }
        autoPreview = newRows
        autoChanges = changes
        autoWarnings = warnings
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(menu?.name ?: "菜單") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isAdjustable || menu?.isCustom == true) {
                        Box {
                            TextButton(onClick = { showMore = true }) {
                                Text("更多")
                            }
                            DropdownMenu(
                                expanded = showMore,
                                onDismissRequest = { showMore = false }
                            ) {
                                if (isAdjustable) {
                                    DropdownMenuItem(
                                        text = { Text("另存為自訂菜單") },
                                        onClick = {
                                            showMore = false
                                            showSaveMenu = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("恢復預設動作") },
                                        enabled = canReset,
                                        onClick = {
                                            showMore = false
                                            showReset = true
                                        }
                                    )
                                }
                                if (menu?.isCustom == true) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "刪除課表",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMore = false
                                            showDeleteMenu = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (menu != null) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = {
                            if (hasEdits) showStartConfirm = true else startSession()
                        },
                        enabled = !creating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (hasEdits) "開始紀錄（已調整）"
                            else "開始紀錄（今天）"
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (menu == null) {
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

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            menu?.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            menu?.note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = lazyListState,
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isAdjustable) {
                    item {
                        TemplateAdjustCard(
                            selected = equipmentFilter,
                            onToggle = { category ->
                                equipmentFilter = if (category in equipmentFilter) {
                                    equipmentFilter - category
                                } else {
                                    equipmentFilter + category
                                }
                            },
                            onAutoAdjust = { buildAutoAdjustment() },
                            onReset = { showReset = true },
                            canReset = canReset
                        )
                    }
                    itemsIndexed(
                        rowList,
                        key = { _, row -> row.menuExerciseId }
                    ) { index, row ->
                        MenuExerciseCard(
                            index = index,
                            row = row,
                            isAdjustable = true,
                            onSwap = { swapTarget = row },
                            onOpenGuide = onOpenGuide,
                            onOpenExercise = { onOpenExercise(row.exerciseId) },
                            onOpenVideo = { openUrl(context, it) },
                            dragHandle = null
                        )
                    }
                } else {
                    itemsIndexed(
                        rowList,
                        key = { _, row -> row.menuExerciseId }
                    ) { index, row ->
                        ReorderableItem(
                            state = reorderState,
                            key = row.menuExerciseId
                        ) { _ ->
                            MenuExerciseCard(
                                index = index,
                                row = row,
                                isAdjustable = false,
                                onSwap = null,
                                onOpenGuide = onOpenGuide,
                                onOpenExercise = { onOpenExercise(row.exerciseId) },
                                onOpenVideo = { openUrl(context, it) },
                                dragHandle = {
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.draggableHandle()
                                    ) {
                                        Icon(
                                            Icons.Filled.Menu,
                                            contentDescription = "拖曳排序"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    swapTarget?.let { row ->
        val current = exercises.firstOrNull { it.id == row.exerciseId }
        ExerciseSwapSheet(
            row = row,
            requirement = ExerciseRole.requirementOf(row, current),
            exercises = exercises,
            initialEquipment = equipmentFilter,
            usedExerciseIds = rowList.map { it.exerciseId }.toSet(),
            onDismiss = { swapTarget = null },
            onSelect = { exercise ->
                val index = rowList.indexOfFirst {
                    it.menuExerciseId == row.menuExerciseId
                }
                if (index >= 0) {
                    rowList[index] = row.copy(
                        exerciseId = exercise.id,
                        name = exercise.name,
                        equipment = exercise.equipment,
                        parts = exercise.parts,
                        videoUrl = exercise.videoUrl,
                        guideId = exercise.guideId
                    )
                    draftTouched = true
                }
                swapTarget = null
            }
        )
    }

    if (showSaveMenu && menu != null) {
        SaveMenuDialog(
            defaultName = "${menu?.name ?: "課表"}・自訂版",
            onDismiss = { showSaveMenu = false },
            onSave = { name ->
                showSaveMenu = false
                scope.launch {
                    repository.createCustomMenu(
                        name = name,
                        subtitle = if (differsFromTemplate) {
                            "由「${menu?.name}」調整"
                        } else {
                            "複製自「${menu?.name}」"
                        },
                        items = rowList.map { row ->
                            NewMenuExerciseInput(
                                exerciseId = row.exerciseId,
                                equipment = row.equipment,
                                targetSets = row.targetSets,
                                targetReps = row.targetReps,
                                restSec = row.restSec,
                                supersetKey = row.supersetKey,
                                slotKey = row.slotKey,
                                role = row.role,
                                laterality = row.laterality,
                                pattern = row.pattern
                            )
                        }
                    )
                    Toast.makeText(context, "已存為自訂菜單", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("恢復預設動作？") },
            text = { Text("會還原成課表預設，並清除記住的上次動作；器材選擇會保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReset = false
                        rowList.clear()
                        rowList.addAll(rows)
                        baselineRows = rows
                        draftTouched = false
                    }
                ) { Text("恢復") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("取消") }
            }
        )
    }

    if (showStartConfirm) {
        val originalById = baselineRows.associateBy { it.menuExerciseId }
        val changes = rowList.mapNotNull { current ->
            val original = originalById[current.menuExerciseId] ?: return@mapNotNull null
            if (original.exerciseId != current.exerciseId) {
                Triple(current.slotKey ?: current.name, original.name, current.name)
            } else {
                null
            }
        }
        StartSessionDialog(
            equipment = equipmentFilter,
            changes = changes,
            onDismiss = { showStartConfirm = false },
            onConfirm = {
                showStartConfirm = false
                startSession()
            }
        )
    }

    autoPreview?.let { preview ->
        AutoAdjustDialog(
            changes = autoChanges,
            warnings = autoWarnings,
            onDismiss = { autoPreview = null },
            onConfirm = {
                rowList.clear()
                rowList.addAll(preview)
                draftTouched = true
                autoPreview = null
            }
        )
    }

    val currentMenu = menu
    if (showDeleteMenu && currentMenu != null) {
        AlertDialog(
            onDismissRequest = { showDeleteMenu = false },
            title = { Text("刪除自訂菜單？") },
            text = {
                Text("「${currentMenu.name}」會從菜單列表移除，但不會影響已建立的訓練紀錄。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteMenu = false
                        scope.launch {
                            repository.deleteMenu(currentMenu)
                            onBack()
                        }
                    }
                ) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMenu = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateAdjustCard(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onAutoAdjust: () -> Unit,
    onReset: () -> Unit,
    canReset: Boolean
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("依今天器材調整", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "只換動作，不動組數、休息與配對順序。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ExerciseMeta.equipmentCategories
                    .filter { it != "有氧" }
                    .forEach { category ->
                        FilterChip(
                            selected = category in selected,
                            onClick = { onToggle(category) },
                            label = { Text(equipmentLabel(category)) }
                        )
                    }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onAutoAdjust, enabled = selected.isNotEmpty()) {
                    Text("一鍵依器材調整")
                }
                if (canReset) {
                    TextButton(onClick = onReset) { Text("恢復預設") }
                }
            }
            if (selected.isEmpty()) {
                Text(
                    "先選今天可用的器材；不選則顯示全部替代動作。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MenuExerciseCard(
    index: Int,
    row: MenuExerciseRow,
    isAdjustable: Boolean,
    onSwap: (() -> Unit)?,
    onOpenGuide: (String) -> Unit,
    onOpenExercise: () -> Unit,
    onOpenVideo: (String) -> Unit,
    dragHandle: (@Composable () -> Unit)?
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = if (isAdjustable) (row.slotKey ?: "${index + 1}") else "${index + 1}",
                    modifier = Modifier.width(36.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f)) {
                    if (isAdjustable && row.role != null) {
                        Text(
                            buildString {
                                append(row.role)
                                row.laterality?.let { append("・$it") }
                                row.pattern?.let { append("・$it") }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                    row.supersetKey?.let {
                        Text(
                            "配對組 $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    val meta = listOfNotNull(
                        row.targetSets?.let { TargetParser.displaySets(it) },
                        row.targetReps?.let { TargetParser.displayReps(it) },
                        row.restSec?.let { TargetParser.displayRest(it) }
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    row.equipment?.let {
                        Text(
                            "器材：$it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    row.parts?.let {
                        Text(
                            "部位：$it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (onSwap != null) {
                    TextButton(onClick = onSwap) { Text("更換") }
                }
                dragHandle?.invoke()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenExercise) {
                    Text("動作詳情")
                }
                row.guideId?.let { guideId ->
                    TextButton(onClick = { onOpenGuide(guideId) }) {
                        Text("動作說明")
                    }
                }
                row.videoUrl?.let { url ->
                    TextButton(onClick = { onOpenVideo(url) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("影片")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseSwapSheet(
    row: MenuExerciseRow,
    requirement: SlotRequirement,
    exercises: List<ExerciseEntity>,
    initialEquipment: Set<String>,
    usedExerciseIds: Set<Long>,
    onDismiss: () -> Unit,
    onSelect: (ExerciseEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf(initialEquipment) }
    var showAll by remember { mutableStateOf(false) }
    val recommended = remember(requirement) {
        SlotRecommendations.namesFor(requirement)
    }
    val candidates = remember(
        exercises, row.exerciseId, requirement, search,
        selectedEquipment, showAll, usedExerciseIds
    ) {
        exercises.asSequence()
            .filter { it.id != row.exerciseId }
            .filter { ExerciseRole.isStrengthCandidate(it) }
            .filter { it.id !in usedExerciseIds }
            .filter {
                search.isBlank() || it.name.contains(search.trim(), ignoreCase = true)
            }
            .filter {
                selectedEquipment.isEmpty() ||
                    ExerciseMeta.equipmentCategory(it.equipment) in selectedEquipment
            }
            .filter { showAll || ExerciseRole.matchesRole(it, requirement.role) }
            .filter {
                showAll || ExerciseRole.lateralityMatches(it, requirement.laterality)
            }
            .sortedWith(
                compareByDescending<ExerciseEntity> { it.name in recommended }
                    .thenByDescending { ExerciseRole.matchScore(it, requirement) }
                    .thenBy {
                        recommended.indexOf(it.name).let { index ->
                            if (index >= 0) index else Int.MAX_VALUE
                        }
                    }
                    .thenBy { it.name }
            )
            .toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "更換 ${row.slotKey ?: row.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        buildString {
                            append(requirement.role)
                            requirement.laterality?.let { append("・$it") }
                            requirement.pattern?.let { append("・$it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) { Text("關閉") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜尋動作") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "器材",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ExerciseMeta.equipmentCategories
                    .filter { it != "有氧" }
                    .forEach { category ->
                        FilterChip(
                            selected = category in selectedEquipment,
                            onClick = {
                                selectedEquipment = if (category in selectedEquipment) {
                                    selectedEquipment - category
                                } else {
                                    selectedEquipment + category
                                }
                            },
                            label = { Text(equipmentLabel(category)) }
                        )
                    }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "顯示全部動作",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "可能不符合課表角色，請自行確認。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showAll,
                    onCheckedChange = { showAll = it }
                )
            }
            Text(
                "共 ${candidates.size} 個動作",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            if (candidates.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("沒有符合的動作", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "試試放寬器材，或開啟「顯示全部動作」。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates, key = { it.id }) { exercise ->
                        ExerciseCandidateCard(
                            exercise = exercise,
                            requirement = requirement,
                            recommended = exercise.name in recommended,
                            onClick = { onSelect(exercise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCandidateCard(
    exercise: ExerciseEntity,
    requirement: SlotRequirement,
    recommended: Boolean,
    onClick: () -> Unit
) {
    val label = ExerciseRole.matchLabel(exercise, requirement)
    val labelColor = when {
        label == "完全符合" -> MaterialTheme.colorScheme.primary
        label.startsWith("部分") -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (recommended) {
                    Text(
                        "推薦",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                "${ExerciseMeta.categoryLabel(exercise)}・" +
                    "${ExerciseMeta.equipmentCategory(exercise.equipment)}・" +
                    ExerciseMeta.difficultyLabel(ExerciseMeta.difficultyOf(exercise)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            exercise.parts?.let {
                Text(
                    "部位：$it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }
    }
}

@Composable
private fun SaveMenuDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存為自訂菜單") },
        text = {
            Column {
                Text("會保留目前的動作、組數、休息與配對順序。")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("菜單名稱") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun StartSessionDialog(
    equipment: Set<String>,
    changes: List<Triple<String, String, String>>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("開始今天的紀錄？") },
        text = {
            Column {
                if (equipment.isNotEmpty()) {
                    Text("今日器材：${equipment.joinToString("、")}")
                    Spacer(Modifier.height(6.dp))
                }
                if (changes.isEmpty()) {
                    Text("將使用目前的課表內容建立紀錄。")
                } else {
                    Text("已調整 ${changes.size} 個動作：")
                    Spacer(Modifier.height(4.dp))
                    changes.forEach { (slot, oldName, newName) ->
                        Text(
                            "$slot：$oldName → $newName",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("開始") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AutoAdjustDialog(
    changes: List<Triple<String, String, String>>,
    warnings: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("依器材調整動作？") },
        text = {
            Column {
                Text("會更換 ${changes.size} 個動作：")
                Spacer(Modifier.height(4.dp))
                changes.forEach { (slot, oldName, newName) ->
                    Text(
                        "$slot：$oldName → $newName",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (warnings.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    warnings.forEach { warning ->
                        Text(
                            warning,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("套用") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun equipmentLabel(category: String): String =
    if (category == "其他") "器械／其他" else category
