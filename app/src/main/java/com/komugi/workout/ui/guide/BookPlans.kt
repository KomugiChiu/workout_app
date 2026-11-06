package com.komugi.workout.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.komugi.workout.data.MenuSummary

/**
 * 書本課表分組：照書（第 3 章＋第 13 章）原順序排列。
 * 可調模板（重訓計畫 A/B、三動作全身）同時出現在菜單頁，這裡只做參考入口。
 */
data class BookPlanGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val menuIds: List<String>
)

val BOOK_PLAN_GROUPS: List<BookPlanGroup> = listOf(
    BookPlanGroup(
        key = "four-week",
        title = "四週減脂計畫",
        subtitle = "第 3 章・Craig Rasmussen",
        menuIds = listOf("plan-a", "plan-b")
    ),
    BookPlanGroup(
        key = "getback",
        title = "恢復訓練",
        subtitle = "Get-Back-in-Shape・Joe Dowdell・12 週",
        menuIds = listOf("getback-p1", "getback-p2", "getback-p3a", "getback-p3b")
    ),
    BookPlanGroup(
        key = "crowded",
        title = "人多健身房",
        subtitle = "Crowded Gym・不換器材",
        menuIds = listOf(
            "crowded-a", "crowded-b", "crowded-c",
            "crowded2-a", "crowded2-b", "crowded2-c",
            "crowded3-a", "crowded3-b"
        )
    ),
    BookPlanGroup(
        key = "fatloss",
        title = "終極減脂",
        subtitle = "Ultimate Fat-Loss・Bill Hartman・12 週",
        menuIds = listOf(
            "fatloss-p1a", "fatloss-p1b", "fatloss-p1c",
            "fatloss-p2a", "fatloss-p2b", "fatloss-p2c",
            "fatloss-p3a", "fatloss-p3b", "fatloss-p3c"
        )
    ),
    BookPlanGroup(
        key = "beach",
        title = "海灘身材",
        subtitle = "Beach-Ready・Vince DelMonte・8 週",
        menuIds = listOf(
            "beach-p1a", "beach-p1b", "beach-p1c",
            "beach-p2a", "beach-p2b", "beach-p2c", "beach-p2d",
            "beach-p3a", "beach-p3b", "beach-p3c",
            "beach-p4a", "beach-p4b", "beach-p4c", "beach-p4d"
        )
    ),
    BookPlanGroup(
        key = "wedding",
        title = "婚禮特訓",
        subtitle = "Wedding・John Alvino・8 週",
        menuIds = listOf(
            "wedding-p1a", "wedding-p1b", "wedding-p1c",
            "wedding-p2a", "wedding-p2b", "wedding-p2c"
        )
    ),
    BookPlanGroup(
        key = "vertical",
        title = "垂直跳躍",
        subtitle = "Vertical-Jump・Kelly Baggett・8 週",
        menuIds = listOf("vertical-p1a", "vertical-p1b", "vertical-p2a", "vertical-p2b")
    ),
    BookPlanGroup(
        key = "scrawny",
        title = "瘦子增肌",
        subtitle = "Scrawny-to-Brawny・Zach Even-Esh",
        menuIds = listOf("scrawny-a", "scrawny-b", "scrawny-c")
    ),
    BookPlanGroup(
        key = "sports",
        title = "運動員",
        subtitle = "Sports・Mike Boyle",
        menuIds = listOf("sports-a", "sports-b", "sports-c", "sports-d")
    ),
    BookPlanGroup(
        key = "threeex",
        title = "三動作全身",
        subtitle = "Three-Exercise・Bill Hartman",
        menuIds = listOf("three-exercise")
    ),
    BookPlanGroup(
        key = "bigbench",
        title = "臥推專項",
        subtitle = "Big Bench・Dave Tate・8 週",
        menuIds = listOf("bigbench-a", "bigbench-b")
    ),
    BookPlanGroup(
        key = "couples",
        title = "雙人燃脂",
        subtitle = "Couples・主課表＋4 分鐘加餐",
        menuIds = listOf(
            "couples-a", "couples-b", "couples-c",
            "couples-f1", "couples-f2", "couples-f3",
            "couples-f4", "couples-f5", "couples-f6"
        )
    ),
    BookPlanGroup(
        key = "bodyweight",
        title = "徒手全身",
        subtitle = "Body-Weight・免器材",
        menuIds = listOf("bw-1", "bw-2", "bw-3")
    ),
    BookPlanGroup(
        key = "min15",
        title = "15 分鐘",
        subtitle = "15-Minute・10 套",
        menuIds = listOf(
            "min15-01", "min15-02", "min15-03", "min15-04", "min15-05",
            "min15-06", "min15-07", "min15-08", "min15-09", "min15-10"
        )
    ),
    BookPlanGroup(
        key = "spartacus",
        title = "斯巴達克斯",
        subtitle = "Spartacus・10 站循環",
        menuIds = listOf("spartacus")
    )
)

@Composable
fun BookPlanGroupCard(
    group: BookPlanGroup,
    summaries: Map<String, MenuSummary>,
    forceExpanded: Boolean,
    onOpenReference: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(group.key) { mutableStateOf(false) }
    val isExpanded = expanded || forceExpanded
    val rows = remember(group, summaries) {
        group.menuIds.mapNotNull { summaries[it] }
    }
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(group.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${group.subtitle}・${rows.size} 份課表",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (isExpanded) "收起" else "展開",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { summary ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        summary.menu.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (summary.menu.isTemplate) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "可調整",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                Text(
                                    "${summary.exerciseCount} 個動作",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (summary.menu.isTemplate) {
                                        onOpenTemplate(summary.menu.id)
                                    } else {
                                        onOpenReference(summary.menu.id)
                                    }
                                }
                            ) {
                                Text(if (summary.menu.isTemplate) "前往調整" else "查看")
                            }
                        }
                    }
                }
            }
        }
    }
}
