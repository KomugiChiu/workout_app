package com.komugi.workout.ui.menu

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.Menu
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.util.openUrl
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuListScreen(
    repository: WorkoutRepository,
    onOpenMenu: (Menu) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("菜單") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "選一份課表開始今天的紀錄；所有內容離線可看。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(repository.menus, key = { it.id }) { menu ->
                Card(
                    onClick = { onOpenMenu(menu) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(menu.name, style = MaterialTheme.typography.titleMedium)
                        menu.subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        AssistChip(
                            onClick = { onOpenMenu(menu) },
                            label = { Text("${menu.exercises.size} 個動作") }
                        )
                    }
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
    onSessionCreated: (Long) -> Unit
) {
    val menu = repository.menuById(menuId)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val creating = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(menu?.name ?: "菜單") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (menu != null) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = {
                            if (!creating.value) {
                                creating.value = true
                                scope.launch {
                                    val id = repository.createSessionFromMenu(
                                        menu,
                                        LocalDate.now().toEpochDay()
                                    )
                                    onSessionCreated(id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("開始紀錄（今天）")
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
                Text("找不到菜單")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                menu.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                menu.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(menu.exercises) { index, exercise ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "${index + 1}",
                                modifier = Modifier.width(28.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                val meta = listOfNotNull(
                                    exercise.sets?.let { "$it 組" },
                                    exercise.reps?.let { "$it 下" },
                                    exercise.rest?.let { "休 $it" }
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                exercise.equipment?.let {
                                    Text(
                                        "器材：$it",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                exercise.parts?.let {
                                    Text(
                                        "部位：$it",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            exercise.guide?.let { guideId ->
                                TextButton(onClick = { onOpenGuide(guideId) }) {
                                    Text("動作說明")
                                }
                            }
                            exercise.video?.let { url ->
                                TextButton(onClick = { openUrl(context, url) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Text("影片")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
