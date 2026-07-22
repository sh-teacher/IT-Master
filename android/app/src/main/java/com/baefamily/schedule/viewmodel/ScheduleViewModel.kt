package com.baefamily.schedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baefamily.schedule.data.model.FamilyRole
import com.baefamily.schedule.data.model.RecurrenceInput
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.data.repository.ScheduleRepository
import com.baefamily.schedule.notification.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ScheduleFormState(
    val title: String = "",
    val memo: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val isAllDay: Boolean = false,
    val recurrenceEnabled: Boolean = false,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val untilDate: LocalDate = LocalDate.now().plusMonths(3),
    val leadMinutes: Int = 10,
    val vibrate: Boolean = true
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository()
    private val zone: ZoneId = ZoneId.systemDefault()
    private val appContext get() = getApplication<Application>()

    private val _form = MutableStateFlow(ScheduleFormState())
    val form: StateFlow<ScheduleFormState> = _form.asStateFlow()

    private val _editingSchedule = MutableStateFlow<Schedule?>(null)
    val editingSchedule: StateFlow<Schedule?> = _editingSchedule.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    fun initForNew(date: LocalDate, defaultLeadMinutes: Int, defaultVibrate: Boolean) {
        _editingSchedule.value = null
        _saveCompleted.value = false
        _form.value = ScheduleFormState(date = date, leadMinutes = defaultLeadMinutes, vibrate = defaultVibrate)
    }

    fun loadForEdit(scheduleId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _saveCompleted.value = false
            val schedule = scheduleRepository.getById(scheduleId)
            _editingSchedule.value = schedule
            if (schedule != null) {
                val start = Instant.ofEpochMilli(schedule.startAt).atZone(zone)
                val end = Instant.ofEpochMilli(schedule.endAt).atZone(zone)
                _form.value = ScheduleFormState(
                    title = schedule.title,
                    memo = schedule.memo,
                    date = start.toLocalDate(),
                    startTime = start.toLocalTime(),
                    endTime = end.toLocalTime(),
                    isAllDay = schedule.isAllDay,
                    leadMinutes = schedule.leadMinutes,
                    vibrate = schedule.vibrate
                )
            }
            _isLoading.value = false
        }
    }

    fun updateForm(update: (ScheduleFormState) -> ScheduleFormState) {
        _form.value = update(_form.value)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun save(ownerUid: String, role: FamilyRole) {
        val state = _form.value
        if (state.title.isBlank()) {
            _errorMessage.value = "제목을 입력해주세요"
            return
        }
        val startMillis = state.date.atTime(state.startTime).atZone(zone).toInstant().toEpochMilli()
        val endMillis = state.date.atTime(state.endTime).atZone(zone).toInstant().toEpochMilli()
        if (endMillis <= startMillis) {
            _errorMessage.value = "종료 시간은 시작 시간 이후여야 해요"
            return
        }
        if (state.recurrenceEnabled && state.daysOfWeek.isEmpty()) {
            _errorMessage.value = "반복할 요일을 선택해주세요"
            return
        }
        if (state.recurrenceEnabled && !state.untilDate.isAfter(state.date)) {
            _errorMessage.value = "반복 종료일은 시작일 이후여야 해요"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val editing = _editingSchedule.value
            if (editing != null) {
                saveEdit(editing, state, startMillis, endMillis, ownerUid)
            } else {
                saveNew(state, startMillis, endMillis, ownerUid, role)
            }
            _isLoading.value = false
        }
    }

    private suspend fun saveEdit(
        editing: Schedule,
        state: ScheduleFormState,
        startMillis: Long,
        endMillis: Long,
        ownerUid: String
    ) {
        // 편집은 반복 여부와 무관하게 항상 이 일정 한 건만 수정한다.
        val updated = editing.copy(
            title = state.title,
            memo = state.memo,
            startAt = startMillis,
            endAt = endMillis,
            isAllDay = state.isAllDay,
            leadMinutes = state.leadMinutes,
            vibrate = state.vibrate
        )
        scheduleRepository.updateSchedule(updated)
            .onSuccess {
                if (updated.ownerUid == ownerUid) {
                    AlarmScheduler.cancel(appContext, updated.id)
                    AlarmScheduler.schedule(appContext, updated)
                }
                _saveCompleted.value = true
            }
            .onFailure { _errorMessage.value = it.message ?: "저장에 실패했습니다." }
    }

    private suspend fun saveNew(
        state: ScheduleFormState,
        startMillis: Long,
        endMillis: Long,
        ownerUid: String,
        role: FamilyRole
    ) {
        val base = Schedule(
            ownerUid = ownerUid,
            role = role,
            title = state.title,
            memo = state.memo,
            startAt = startMillis,
            endAt = endMillis,
            isAllDay = state.isAllDay,
            leadMinutes = state.leadMinutes,
            vibrate = state.vibrate
        )
        if (state.recurrenceEnabled) {
            scheduleRepository.addRecurringSchedule(base, RecurrenceInput(state.daysOfWeek, state.untilDate))
                .onSuccess { saved ->
                    saved.forEach { AlarmScheduler.schedule(appContext, it) }
                    _saveCompleted.value = true
                }
                .onFailure { _errorMessage.value = it.message ?: "저장에 실패했습니다." }
        } else {
            scheduleRepository.addSingleSchedule(base)
                .onSuccess { saved ->
                    AlarmScheduler.schedule(appContext, saved)
                    _saveCompleted.value = true
                }
                .onFailure { _errorMessage.value = it.message ?: "저장에 실패했습니다." }
        }
    }

    fun deleteSingle(onDone: () -> Unit) {
        val schedule = _editingSchedule.value ?: return
        viewModelScope.launch {
            scheduleRepository.deleteSingle(schedule.id)
                .onSuccess {
                    AlarmScheduler.cancel(appContext, schedule.id)
                    onDone()
                }
                .onFailure { _errorMessage.value = it.message ?: "삭제에 실패했습니다." }
        }
    }

    fun deleteThisAndFollowing(onDone: () -> Unit) {
        val schedule = _editingSchedule.value ?: return
        val groupId = schedule.recurrenceGroupId ?: return
        viewModelScope.launch {
            scheduleRepository.deleteThisAndFollowing(groupId, schedule.startAt)
                .onSuccess { deletedIds ->
                    AlarmScheduler.cancelAll(appContext, deletedIds)
                    onDone()
                }
                .onFailure { _errorMessage.value = it.message ?: "삭제에 실패했습니다." }
        }
    }

    fun deleteEntireSeries(onDone: () -> Unit) {
        val schedule = _editingSchedule.value ?: return
        val groupId = schedule.recurrenceGroupId ?: return
        viewModelScope.launch {
            scheduleRepository.deleteEntireSeries(groupId)
                .onSuccess { deletedIds ->
                    AlarmScheduler.cancelAll(appContext, deletedIds)
                    onDone()
                }
                .onFailure { _errorMessage.value = it.message ?: "삭제에 실패했습니다." }
        }
    }
}
