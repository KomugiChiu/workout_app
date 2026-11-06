package com.komugi.workout.ui.guide

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.komugi.workout.ui.components.isFullBodyExercise
import com.komugi.workout.ui.components.muscleGroupLabel
import com.komugi.workout.ui.components.muscleGroupsIn
import com.komugi.workout.ui.theme.komugiTopAppBarColors

/**
 * 肌肉部位獨立頁：依 13 個部位分群瀏覽動作，不再放在動作說明內。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleGroupsScreen(
    repository: WorkoutRepository,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenAtlas: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val exercises by repository.observeExercises().collectAsState(initial = emptyList())
    val keyword = query.trim()
    val groups = remember(keyword, exercises) {
        MuscleGroup.entries.mapNotNull { group ->
            val matched = exercises.filter { exercise ->
                (keyword.isBlank() || exercise.name.contains(keyword, ignoreCase = true)) &&
                    if (group == MuscleGroup.FULL_BODY) {
                        isFullBodyExercise(exercise.parts)
                    } else {
                        muscleGroupsIn(exercise.parts, limit = 10).contains(group)
                    }
            }
            if (matched.isEmpty()) null else group to matched.sortedBy { it.name }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("肌肉部位") },
                colors = komugiTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜尋動作…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true
            )
            if (groups.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "找不到符合的動作",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (keyword.isBlank()) {
                        item {
                            Card(
                                onClick = onOpenAtlas,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "肌肉部位說明全圖鑑",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "圖解各部位位置與功能",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    items(groups, key = { it.first }) { (group, matched) ->
                        MuscleGroupCard(
                            group = group,
                            exercises = matched,
                            forceExpanded = keyword.isNotBlank(),
                            onOpenExercise = onOpenExercise
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupCard(
    group: MuscleGroup,
    exercises: List<ExerciseEntity>,
    forceExpanded: Boolean,
    onOpenExercise: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(group) { mutableStateOf(false) }
    val isExpanded = expanded || forceExpanded
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    muscleGroupLabel(group),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${exercises.size} 個動作",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isExpanded) "收起" else "展開",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercises.forEach { exercise ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    exercise.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${ExerciseMeta.categoryLabel(exercise)}・" +
                                        ExerciseMeta.equipmentCategory(exercise.equipment),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onOpenExercise(exercise.id) }) {
                                Text("查看")
                            }
                        }
                    }
                }
            }
        }
    }
}
