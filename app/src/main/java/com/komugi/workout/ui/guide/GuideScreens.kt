package com.komugi.workout.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.GuideRef
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.util.MarkdownWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideListScreen(
    repository: WorkoutRepository,
    onOpenGuide: (GuideRef) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("動作說明") }) }
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
                    "每個動作的完整拆解：影片、流程、檢查點、常見錯誤。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(repository.guides, key = { it.id }) { guide ->
                Card(
                    onClick = { onOpenGuide(guide) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp)
                    ) {
                        Text(guide.title, style = MaterialTheme.typography.titleMedium)
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
        MarkdownWebView(
            markdown = markdown,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
