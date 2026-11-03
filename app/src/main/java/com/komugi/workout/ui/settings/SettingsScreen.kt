package com.komugi.workout.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.theme.AppTheme
import com.komugi.workout.ui.theme.ThemeManager
import com.komugi.workout.ui.theme.ThemeMode
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.openUrl
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: WorkoutRepository,
    themeManager: ThemeManager
) {
    val currentTheme by themeManager.theme.collectAsState()
    val currentMode by themeManager.mode.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val text = repository.exportBackupJson()
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(text.toByteArray(Charsets.UTF_8))
                        } ?: error("無法寫入檔案")
                    }
                }.onSuccess {
                    Toast.makeText(context, "已匯出備份", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(
                        context,
                        "匯出失敗：${it.message ?: "未知錯誤"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importUri = uri
            showImportConfirm = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("選項") }, colors = komugiTopAppBarColors()) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("主題顏色", style = MaterialTheme.typography.titleMedium)
                Text(
                    "選擇喜歡的色系，立即套用到整個 App",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(AppTheme.entries.size) { index ->
                val theme = AppTheme.entries[index]
                Card(
                    onClick = { themeManager.setTheme(theme) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(theme.previewColor)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(theme.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                theme.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (currentTheme == theme) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "使用中",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("外觀模式", style = MaterialTheme.typography.titleMedium)
                Text(
                    "跟隨系統，或固定淺色／深色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = currentMode == mode,
                            onClick = { themeManager.setMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("資料備份", style = MaterialTheme.typography.titleMedium)
                Text(
                    "匯出 JSON 備份，或從備份還原。匯入會覆蓋目前所有訓練、菜單與動作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            exportLauncher.launch("komugi-backup-${LocalDate.now()}.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("匯出備份")
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream")
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("匯入備份")
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("關於", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            openUrl(context, "https://github.com/KomugiChiu/workout_app")
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Komugi Workout", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "v1.0 · github.com/KomugiChiu/workout_app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val pendingImport = importUri
    if (showImportConfirm && pendingImport != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                importUri = null
            },
            title = { Text("匯入備份？") },
            text = {
                Text("目前的訓練紀錄、菜單、動作與自訂內容會全部被備份檔取代。建議先匯出目前資料。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val text = context.contentResolver
                                        .openInputStream(pendingImport)
                                        ?.bufferedReader()
                                        ?.use { it.readText() }
                                        ?: error("無法讀取檔案")
                                    repository.importBackupJson(text)
                                }
                            }.onSuccess {
                                Toast.makeText(
                                    context,
                                    "已匯入備份",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    "匯入失敗：${it.message ?: "未知錯誤"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            importUri = null
                        }
                    }
                ) {
                    Text("匯入並覆蓋", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        importUri = null
                    }
                ) { Text("取消") }
            }
        )
    }
}
