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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.GuideRef
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.components.MuscleGroup
import com.komugi.workout.ui.components.MuscleMap
import com.komugi.workout.ui.components.muscleGroupsIn
import com.komugi.workout.ui.theme.komugiTopAppBarColors
import com.komugi.workout.util.MarkdownWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideListScreen(
    repository: WorkoutRepository,
    onOpenGuide: (GuideRef) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val guideTexts = remember(repository.guides) {
        repository.guides.associate { guide ->
            guide.id to runCatching { repository.readGuide(guide.asset) }.getOrDefault("")
        }
    }

    // 肌肉部位已獨立成資料頁入口，這裡不再列出全圖鑑
    val guideSource = remember(repository.guides) {
        repository.guides.filterNot { it.id == "muscles" }
    }

    val filtered = remember(query, guideTexts) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            guideSource
        } else {
            guideSource.filter { guide ->
                guide.title.contains(keyword, ignoreCase = true) ||
                    guide.subtitle?.contains(keyword, ignoreCase = true) == true ||
                    guide.parts?.contains(keyword, ignoreCase = true) == true ||
                    guideTexts[guide.id]?.contains(keyword, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動作說明") },
                colors = komugiTopAppBarColors(),
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
                colors = komugiTopAppBarColors(),
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
            if (guide?.id == "muscles") {
                MuscleAtlasScreen(
                    markdown = markdown,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                guide?.parts?.let { parts ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        muscleGroupsIn(parts, limit = 3).forEach { group ->
                            MuscleMap(
                                group = group,
                                modifier = Modifier.size(44.dp, 60.dp)
                            )
                            Spacer(Modifier.width(8.dp))
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
}

private data class MuscleAtlasEntry(
    val group: MuscleGroup,
    val title: String,
    val anatomy: String,
    val role: String,
    val exercises: String
)

private val AtlasEntries = listOf(
    MuscleAtlasEntry(
        group = MuscleGroup.CHEST,
        title = "胸（前）— 胸大肌",
        anatomy = "胸口正面，分上胸（鎖骨頭）與中下胸（胸骨頭）",
        role = "把手臂往胸前推、抱合（水平內收）",
        exercises = "地板臥推、飛鳥（全分支）、伏地挺身、上斜伏地挺身"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.BACK,
        title = "背（後）— 背闊・菱形・斜方・豎脊",
        anatomy = "腋下往腰的 V 形背闊＋肩胛間菱形／中下斜方＋脊椎兩側豎脊肌",
        role = "把手臂往後／往下拉、肩胛後收下壓、維持背平",
        exercises = "單手／壺鈴／彈力帶划船、直臂上拉、Y-T-W、RDL（穩定）"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.SHOULDERS,
        title = "肩（前／側／後）— 三角肌",
        anatomy = "前束（前側）、中束（外側，決定肩寬）、後束（後側）＋深層旋轉肌群",
        role = "手臂前舉、側平舉、水平外展與肩關節穩定",
        exercises = "側平舉、單手肩推、胸前蹲推、反向飛鳥、Y-T-W"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.BICEPS,
        title = "二頭（前）— 肱二頭肌＋肱肌",
        anatomy = "上臂前側；肱肌在二頭下方，把二頭頂起來增加手臂厚度",
        role = "屈肘＋前臂旋後",
        exercises = "三停彎舉、錘式彎舉、划船（輔助）"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.TRICEPS,
        title = "三頭（後）— 肱三頭肌",
        anatomy = "佔上臂體積 60%：長頭（跨肩關節）、外側頭（馬蹄形）、內側頭",
        role = "伸肘；手臂舉過頭時長頭拉伸最大",
        exercises = "三停三頭肌伸展、啞鈴過頭三頭伸展、臥推／伏地挺身（輔助）"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.FOREARMS,
        title = "前臂（前／外）— 肱橈肌＋屈伸肌群",
        anatomy = "前臂外側肱橈肌＋內側屈肌（抓握）＋外側伸肌（穩定手腕）",
        role = "抓握負重、維持手腕中立",
        exercises = "農夫行走、行李箱搬運、錘式彎舉、硬舉／RDL"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.CORE,
        title = "核心（前／側）— 腹直・腹斜・腹橫",
        anatomy = "正面腹直肌、兩側腹斜肌、深層天然護腰腹橫肌",
        role = "抗伸展（不塌腰）、旋轉與抗旋轉、抗側屈、腹內壓",
        exercises = "死蟲式、鳥狗式、俄羅斯旋轉、換手摸肩、側棒式"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.GLUTES,
        title = "臀（後）— 臀大肌＋臀中肌",
        anatomy = "臀部後側（臀大肌）與骨盆外側（臀中肌）",
        role = "髖伸展（站起、頂髖）、單腳站立穩定骨盆防膝內倒",
        exercises = "六角槓硬舉、RDL、壺鈴擺盪、保加利亞分腿蹲、單腳登階"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.QUADS,
        title = "股四頭（前）— 大腿前側",
        anatomy = "股直肌（正中，兼屈髖）、股外側肌、股內側肌（膝內上方淚滴）、股中間肌",
        role = "伸膝（蹲下站起、上下樓梯）",
        exercises = "高腳杯深蹲、踩帶深蹲、胸前蹲推、保加利亞分腿蹲、登階"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.HAMSTRINGS,
        title = "腿後腱（後）— 大腿後側",
        anatomy = "股二頭肌、半腱肌、半膜肌，跨過髖與膝兩個關節",
        role = "髖伸展（與臀合作）＋膝彎曲",
        exercises = "RDL、六角槓硬舉、壺鈴擺盪、TRX 腿後腱彎舉"
    ),
    MuscleAtlasEntry(
        group = MuscleGroup.CALVES,
        title = "小腿（後）— 腓腸肌＋比目魚肌",
        anatomy = "表層兩顆腓腸肌＋深層耐力型比目魚肌",
        role = "踝蹠屈（推進、跳躍落地緩衝、行走穩定）",
        exercises = "單腳登階、深蹲跳、農夫行走"
    )
)

@Composable
private fun MuscleAtlasScreen(
    markdown: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "局部圖示一覽（主題色＝目標肌群，灰＝身體部位）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(AtlasEntries, key = { it.group.name }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MuscleMap(
                        group = item.group,
                        modifier = Modifier.size(64.dp, 90.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "位置：${item.anatomy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "功能：${item.role}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "動作：${item.exercises}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(
                "前／後與推／拉平衡原則：前面練多少，後面就練多少——姿勢端正、關節不痛的關鍵。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
