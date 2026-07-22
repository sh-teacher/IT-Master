package com.baefamily.schedule.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.ui.common.ScheduleListItem
import com.baefamily.schedule.viewmodel.TimelineViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TimelineScreen(viewModel: TimelineViewModel) {
    val schedules by viewModel.upcomingSchedules.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val now by viewModel.now.collectAsState()
    val zone = ZoneId.systemDefault()

    val grouped = remember(schedules, now) {
        schedules
            .filter { it.endAt >= now }
            .sortedBy { it.startAt }
            .groupBy { Instant.ofEpochMilli(it.startAt).atZone(zone).toLocalDate() }
            .toSortedMap()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("가족 타임라인") }) }) { padding ->
        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "앞으로 일주일간 예정된 일정이 없어요",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                grouped.forEach { (date, daySchedules) ->
                    item(key = "header_$date") {
                        Text(
                            dateLabel(date),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(daySchedules, key = { it.id }) { schedule ->
                        ScheduleListItem(
                            schedule = schedule,
                            ownerName = ownerLabel(schedule, familyMembers),
                            showRemaining = true
                        )
                    }
                }
            }
        }
    }
}

private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "오늘 · ${date.monthValue}월 ${date.dayOfMonth}일"
        today.plusDays(1) -> "내일 · ${date.monthValue}월 ${date.dayOfMonth}일"
        else -> "${date.monthValue}월 ${date.dayOfMonth}일 (${weekdayKorean(date.dayOfWeek)})"
    }
}

private fun weekdayKorean(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

private fun ownerLabel(schedule: Schedule, familyMembers: List<UserProfile>): String =
    familyMembers.find { it.uid == schedule.ownerUid }?.name ?: schedule.role.displayName
