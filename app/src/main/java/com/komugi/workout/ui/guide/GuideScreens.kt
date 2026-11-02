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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.GuideRef
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.components.MuscleMap
import com.komugi.workout.ui.components.primaryMuscleGroup
import com.komugi.workout.util.MarkdownWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideListScreen(
    repository: WorkoutRepository,
    onOpenGuide: (GuideRef) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val guideTexts = remember(repository.guides) {
        repository.guides.associate { guide ->
            guide.id to runCatching { repository.readGuide(guide.asset) }.getOrDefault("")
        }
    }

    val filtered = remember(query, guideTexts) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            repository.guides
        } else {
            repository.guides.filter { guide ->
                guide.title.contains(keyword, ignoreCase = true) ||
                    guide.subtitle?.contains(keyword, ignoreCase = true) == true ||
                    guide.parts?.contains(keyword, ignoreCase = true) == true ||
                    guideTexts[guide.id]?.contains(keyword, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("動作說明") }) }
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
                placeholder = { Text("搜尋動作名稱或內文…") },
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

            if (query.isNotBlank()) {
                Text(
                    "找到 ${filtered.size} 筆",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }

            if (filtered.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "找不到符合的動作說明",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { guide ->
                        Card(
                            onClick = { onOpenGuide(guide) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(guide.title, style = MaterialTheme.typography.titleMedium)
                                guide.parts?.let { parts ->
                                    Text(
                                        "部位：$parts",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                guide.subtitle?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideDetailScreen(
    repository: WorkoutRepository,
    guideId: String?,
    onBack: () -> Unit
) {
    val guide = repository.guideById(guideId)
    val markdown = remember(guideId) {
        guide?.let { ref ->
            runCatching { repository.readGuide(ref.asset) }
                .getOrElse { "無法載入內容：${it.message}" }
        } ?: "找不到這份動作說明。"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guide?.title ?: "動作說明") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            guide?.parts?.let { parts ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    primaryMuscleGroup(parts)?.let { group ->
                        MuscleMap(
                            group = group,
                            modifier = Modifier.size(46.dp, 64.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            "訓練部位",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            parts,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                HorizontalDivider()
            }

            MarkdownWebView(
                markdown = markdown,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}
