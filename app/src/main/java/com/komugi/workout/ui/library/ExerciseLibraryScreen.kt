package com.komugi.workout.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.ExerciseEntity
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.components.ExerciseMeta
import com.komugi.workout.ui.components.MuscleGroup
import com.komugi.workout.ui.components.muscleGroupLabel
import com.komugi.workout.ui.theme.komugiTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    repository: WorkoutRepository,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val exercises by repository.observeExercises().collectAsState(initial = emptyList())
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<MuscleGroup?>(null) }
    var type by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf<String?>(null) }
    var equipment by remember { mutableStateOf<String?>(null) }
    var showTags by remember { mutableStateOf(false) }

    val filtered = remember(exercises, search, category, type, location, equipment) {
        val keyword = search.trim()
        exercises.filter { exercise ->
            (keyword.isBlank() || exercise.name.contains(keyword, ignoreCase = true)) &&
                (category == null || ExerciseMeta.categoryOf(exercise) == category) &&
                (type == null || ExerciseMeta.movementTypeOf(exercise) == type) &&
                (location == null || ExerciseMeta.locationOf(exercise) == location) &&
                (equipment == null ||
                    ExerciseMeta.equipmentCategory(exercise.equipment) == equipment)
        }
    }
    val hasFilter = search.isNotBlank() || category != null || type != null ||
        location != null || equipment != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動作庫") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜尋動作") },
                    singleLine = true
                )
            }
            item {
                LibraryFilterRow(
                    title = "分類",
                    options = listOf<MuscleGroup?>(null) + MuscleGroup.entries,
                    selected = category,
                    label = { it?.let { group -> muscleGroupLabel(group) } ?: "全部" },
                    onSelect = { category = it }
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "標籤",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showTags = !showTags }) {
                        Text(if (showTags) "收起標籤" else "更多標籤")
                    }
                }
            }
            if (showTags) {
                item {
                    LibraryFilterRow(
                        title = "型態",
                        options = listOf<String?>(null) + ExerciseMeta.movementTypes,
                        selected = type,
                        label = { it?.let { value -> ExerciseMeta.movementTypeLabel(value) } ?: "全部" },
                        onSelect = { type = it }
                    )
                }
                item {
                    LibraryFilterRow(
                        title = "地點",
                        options = listOf<String?>(null) + ExerciseMeta.locations,
                        selected = location,
                        label = { it?.let { value -> ExerciseMeta.locationLabel(value) } ?: "全部" },
                        onSelect = { location = it }
                    )
                }
                item {
                    LibraryFilterRow(
                        title = "器材",
                        options = listOf<String?>(null) + ExerciseMeta.equipmentCategories,
                        selected = equipment,
                        label = { it ?: "全部" },
                        onSelect = { equipment = it }
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "共 ${filtered.size} 個動作",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasFilter) {
                        TextButton(
                            onClick = {
                                search = ""
                                category = null
                                type = null
                                location = null
                                equipment = null
                            }
                        ) { Text("清除") }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("沒有符合的動作", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "試試其他關鍵字，或放寬分類與標籤。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { exercise ->
                    ExerciseLibraryRow(
                        exercise = exercise,
                        onClick = { onOpenExercise(exercise.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> LibraryFilterRow(
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
private fun ExerciseLibraryRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            Text(
                "${ExerciseMeta.categoryLabel(exercise)} · " +
                    "${ExerciseMeta.movementTypeLabel(ExerciseMeta.movementTypeOf(exercise))} · " +
                    ExerciseMeta.locationLabel(ExerciseMeta.locationOf(exercise)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "器材：${exercise.equipment ?: "未標記"} · " +
                    "難度：${ExerciseMeta.difficultyLabel(ExerciseMeta.difficultyOf(exercise))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryDetailScreen(
    repository: WorkoutRepository,
    exerciseId: Long,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenGuide: (String) -> Unit
) {
    val exercises by repository.observeExercises().collectAsState(initial = emptyList())
    val exercise = exercises.firstOrNull { it.id == exerciseId }
    val alternatives = remember(exercise, exercises) {
        exercise?.let { ExerciseMeta.alternativesFor(it, exercises) } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動作詳細") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (exercise == null) {
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

        val steps = exercise.instructions
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (exercise.isCustom) {
                                Text(
                                    "自訂",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            ExerciseMeta.categoryLabel(exercise),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            listOf(
                                ExerciseMeta.movementTypeLabel(ExerciseMeta.movementTypeOf(exercise)),
                                ExerciseMeta.locationLabel(ExerciseMeta.locationOf(exercise)),
                                "難度 ${ExerciseMeta.difficultyLabel(ExerciseMeta.difficultyOf(exercise))}"
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        exercise.parts?.let {
                            Text(
                                "部位：$it",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            "器材：${exercise.equipment ?: "未標記"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("步驟", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        if (steps.isNotEmpty()) {
                            steps.forEachIndexed { index, step ->
                                Row(Modifier.padding(vertical = 3.dp)) {
                                    Text(
                                        "${index + 1}.",
                                        modifier = Modifier.width(24.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(step, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            Text(
                                if (exercise.isCustom) {
                                    "自訂動作尚無內建步驟，可在紀錄中補上要點。"
                                } else {
                                    "尚無詳細步驟資料。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        exercise.guideId?.let { guideId ->
                            TextButton(onClick = { onOpenGuide(guideId) }) {
                                Text("查看動作說明")
                            }
                        }
                    }
                }
            }
            exercise.warning?.takeIf { it.isNotBlank() }?.let { warning ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "注意事項",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(warning, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text(
                    "替代動作",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (alternatives.isEmpty()) {
                item {
                    Text(
                        "目前沒有同分類的替代動作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(alternatives, key = { it.id }) { alternative ->
                    Card(
                        onClick = { onOpenExercise(alternative.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                alternative.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${ExerciseMeta.categoryLabel(alternative)} · " +
                                    "${ExerciseMeta.movementTypeLabel(ExerciseMeta.movementTypeOf(alternative))} · " +
                                    ExerciseMeta.locationLabel(ExerciseMeta.locationOf(alternative)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "器材：${alternative.equipment ?: "未標記"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
