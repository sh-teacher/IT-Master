package com.baefamily.schedule.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baefamily.schedule.data.model.FamilyRole
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.ui.common.ScheduleListItem
import com.baefamily.schedule.ui.theme.toColor
import com.baefamily.schedule.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onAddSchedule: (LocalDate) -> Unit,
    onScheduleClick: (Schedule) -> Unit
) {
    val yearMonth by viewModel.yearMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val monthSchedules by viewModel.monthSchedules.collectAsState()
    val selectedDateSchedules by viewModel.selectedDateSchedules.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddSchedule(selectedDate) }) {
                Icon(Icons.Default.Add, contentDescription = "일정 추가")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthHeader(
                yearMonth = yearMonth,
                onPrevious = viewModel::goToPreviousMonth,
                onNext = viewModel::goToNextMonth
            )
            WeekdayHeader()
            MonthGrid(
                yearMonth = yearMonth,
                selectedDate = selectedDate,
                schedules = monthSchedules,
                onDateClick = viewModel::selectDate
            )

            Text(
                text = "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 일정",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            if (selectedDateSchedules.isEmpty()) {
                Text(
                    "등록된 일정이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    items(selectedDateSchedules, key = { it.id }) { schedule ->
                        ScheduleListItem(
                            schedule = schedule,
                            ownerName = ownerLabel(schedule, familyMembers),
                            onClick = { onScheduleClick(schedule) }
                        )
                    }
                }
            }
        }
    }
}

private fun ownerLabel(schedule: Schedule, familyMembers: List<UserProfile>): String =
    familyMembers.find { it.uid == schedule.ownerUid }?.name ?: schedule.role.displayName

@Composable
private fun MonthHeader(yearMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
        }
        Text(
            "${yearMonth.year}년 ${yearMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        WEEKDAY_LABELS.forEach { label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    schedules: List<Schedule>,
    onDateClick: (LocalDate) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val rolesByDate: Map<LocalDate, Set<FamilyRole>> = schedules
        .groupBy { java.time.Instant.ofEpochMilli(it.startAt).atZone(zone).toLocalDate() }
        .mapValues { (_, list) -> list.map { it.role }.toSet() }

    val firstOfMonth = yearMonth.atDay(1)
    val firstDayIndex = firstOfMonth.dayOfWeek.value % 7
    val gridStart = firstOfMonth.minusDays(firstDayIndex.toLong())
    val dates = (0 until 42).map { gridStart.plusDays(it.toLong()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inCurrentMonth = YearMonth.from(date) == yearMonth,
                        isSelected = date == selectedDate,
                        isToday = date == LocalDate.now(),
                        roles = rolesByDate[date].orEmpty(),
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    roles: Set<FamilyRole>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(0.85f)
            .padding(2.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    !inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            roles.take(4).forEach { role ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(role.toColor())
                )
            }
        }
    }
}
