package com.komugi.workout.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/**
 * 自製月曆：支援月份切換、選取日期、今天標示與「有紀錄」小圓點。
 */
@Composable
fun MonthCalendar(
    selectedEpochDay: Long,
    epochDaysWithRecords: Set<Long>,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    var displayMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    LaunchedEffect(selectedEpochDay) {
        val month = YearMonth.from(LocalDate.ofEpochDay(selectedEpochDay))
        if (month != displayMonth) {
            displayMonth = month
        }
    }

    val today = LocalDate.now()
    val swipeThreshold = with(LocalDensity.current) { 48.dp.toPx() }

    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(displayMonth) {
                var dragAmount = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragAmount += amount
                    },
                    onDragEnd = {
                        if (dragAmount > swipeThreshold) {
                            displayMonth = displayMonth.minusMonths(1)
                        } else if (dragAmount < -swipeThreshold) {
                            displayMonth = displayMonth.plusMonths(1)
                        }
                        dragAmount = 0f
                    },
                    onDragCancel = { dragAmount = 0f }
                )
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${displayMonth.year} 年 ${displayMonth.monthValue} 月",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (displayMonth != YearMonth.from(today) || selectedDate != today) {
                TextButton(
                    onClick = {
                        displayMonth = YearMonth.from(today)
                        onSelect(today.toEpochDay())
                    }
                ) { Text("今天") }
            }
            IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上個月")
            }
            IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下個月")
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        val firstOfMonth = displayMonth.atDay(1)
        val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
        val daysInMonth = displayMonth.lengthOfMonth()
        val cellCount = leadingBlanks + daysInMonth
        val rows = (cellCount + 6) / 7

        for (row in 0 until rows) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                for (col in 0 until 7) {
                    val dayNumber = row * 7 + col - leadingBlanks + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = displayMonth.atDay(dayNumber)
                        val epoch = date.toEpochDay()
                        DayCell(
                            day = dayNumber,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            hasRecord = epoch in epochDaysWithRecords,
                            onClick = { onSelect(epoch) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasRecord) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}
