package com.baefamily.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.data.repository.ScheduleRepository
import com.baefamily.schedule.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val LOOKAHEAD_DAYS = 7L

class TimelineViewModel(
    private val scheduleRepository: ScheduleRepository = ScheduleRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    val upcomingSchedules: StateFlow<List<Schedule>> = run {
        val today = LocalDate.now()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(LOOKAHEAD_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        scheduleRepository.observeRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val familyMembers: StateFlow<List<UserProfile>> = userRepository.observeFamilyMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Drives periodic re-computation of "n시간 후" style remaining-time labels.
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                _now.value = System.currentTimeMillis()
            }
        }
    }
}
