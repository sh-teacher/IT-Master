package com.baefamily.schedule.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.ui.common.LeadMinutesDropdown
import com.baefamily.schedule.viewmodel.ScheduleViewModel
import java.time.DayOfWeek
import java.time.LocalDate

private val WEEKDAY_ORDER = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)
private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScheduleScreen(
    viewModel: ScheduleViewModel,
    currentUser: UserProfile,
    scheduleId: String?,
    initialDate: LocalDate,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val form by viewModel.form.collectAsState()
    val editingSchedule by viewModel.editingSchedule.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val saveCompleted by viewModel.saveCompleted.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showUntilDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            viewModel.loadForEdit(scheduleId)
        } else {
            viewModel.initForNew(
                initialDate,
                currentUser.notificationSettings.defaultLeadMinutes,
                currentUser.notificationSettings.vibrate
            )
        }
    }

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) onDone()
    }

    val isOwner = editingSchedule?.let { it.ownerUid == currentUser.uid } ?: true
    val isRecurring = editingSchedule?.recurrenceGroupId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scheduleId == null) "일정 추가" else "일정 상세") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                actions = {
                    if (scheduleId != null && isOwner) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = { value -> viewModel.updateForm { it.copy(title = value) } },
                label = { Text("제목") },
                enabled = isOwner,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.memo,
                onValueChange = { value -> viewModel.updateForm { it.copy(memo = value) } },
                label = { Text("메모") },
                enabled = isOwner,
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                enabled = isOwner,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("날짜: ${form.date}")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("종일", modifier = Modifier.weight(1f))
                Switch(
                    checked = form.isAllDay,
                    onCheckedChange = { value -> viewModel.updateForm { it.copy(isAllDay = value) } },
                    enabled = isOwner
                )
            }

            if (!form.isAllDay) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        enabled = isOwner,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("시작 %02d:%02d".format(form.startTime.hour, form.startTime.minute))
                    }
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        enabled = isOwner,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("종료 %02d:%02d".format(form.endTime.hour, form.endTime.minute))
                    }
                }
            }

            if (scheduleId == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("매주 반복", modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.recurrenceEnabled,
                        onCheckedChange = { value -> viewModel.updateForm { it.copy(recurrenceEnabled = value) } }
                    )
                }
                if (form.recurrenceEnabled) {
                    Text("반복 요일", style = MaterialTheme.typography.labelMedium)
                    WeekdaySelector(
                        selected = form.daysOfWeek,
                        onToggle = { day ->
                            viewModel.updateForm {
                                val newDays = if (day in it.daysOfWeek) it.daysOfWeek - day else it.daysOfWeek + day
                                it.copy(daysOfWeek = newDays)
                            }
                        }
                    )
                    OutlinedButton(onClick = { showUntilDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("반복 종료일: ${form.untilDate}")
                    }
                    Text(
                        "종료일까지 매주 자동으로 등록돼요. 나중에 원하는 날짜부터 한번에 삭제할 수 있어요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isRecurring) {
                Text(
                    "반복 일정의 일부예요. 수정하면 이 일정만 변경돼요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Text("알림 설정", style = MaterialTheme.typography.titleSmall)
            LeadMinutesDropdown(
                selected = form.leadMinutes,
                enabled = isOwner,
                onSelect = { value -> viewModel.updateForm { it.copy(leadMinutes = value) } }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("진동", modifier = Modifier.weight(1f))
                Switch(
                    checked = form.vibrate,
                    onCheckedChange = { value -> viewModel.updateForm { it.copy(vibrate = value) } },
                    enabled = isOwner
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (isOwner) {
                Button(
                    onClick = { viewModel.save(currentUser.uid, currentUser.role) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("저장")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = form.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date -> viewModel.updateForm { it.copy(date = date) }; showDatePicker = false }
        )
    }
    if (showUntilDatePicker) {
        AppDatePickerDialog(
            initialDate = form.untilDate,
            onDismiss = { showUntilDatePicker = false },
            onConfirm = { date -> viewModel.updateForm { it.copy(untilDate = date) }; showUntilDatePicker = false }
        )
    }
    if (showStartTimePicker) {
        AppTimePickerDialog(
            initialTime = form.startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time -> viewModel.updateForm { it.copy(startTime = time) }; showStartTimePicker = false }
        )
    }
    if (showEndTimePicker) {
        AppTimePickerDialog(
            initialTime = form.endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time -> viewModel.updateForm { it.copy(endTime = time) }; showEndTimePicker = false }
        )
    }
    if (showDeleteDialog) {
        DeleteScheduleDialog(
            isRecurring = isRecurring,
            onDismiss = { showDeleteDialog = false },
            onDeleteSingle = { showDeleteDialog = false; viewModel.deleteSingle(onDone) },
            onDeleteFollowing = { showDeleteDialog = false; viewModel.deleteThisAndFollowing(onDone) },
            onDeleteAll = { showDeleteDialog = false; viewModel.deleteEntireSeries(onDone) }
        )
    }
}

@Composable
private fun WeekdaySelector(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WEEKDAY_ORDER.forEachIndexed { index, day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(WEEKDAY_LABELS[index]) }
            )
        }
    }
}
