package com.baefamily.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.data.repository.ScheduleRepository
import com.baefamily.schedule.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val scheduleRepository: ScheduleRepository = ScheduleRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val monthSchedules: StateFlow<List<Schedule>> = _yearMonth
        .flatMapLatest { ym ->
            val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            scheduleRepository.observeRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val familyMembers: StateFlow<List<UserProfile>> = userRepository.observeFamilyMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateSchedules: StateFlow<List<Schedule>> = combine(monthSchedules, selectedDate) { schedules, date ->
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        schedules.filter { it.startAt in dayStart until dayEnd }.sortedBy { it.startAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun goToPreviousMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _yearMonth.value = _yearMonth.value.plusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _yearMonth.value) {
            _yearMonth.value = YearMonth.from(date)
        }
    }
}
