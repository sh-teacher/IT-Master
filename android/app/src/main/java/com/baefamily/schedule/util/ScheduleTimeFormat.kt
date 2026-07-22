package com.baefamily.schedule.util

import com.baefamily.schedule.data.model.Schedule
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val zone = ZoneId.systemDefault()

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)

fun timeRangeLabel(schedule: Schedule): String =
    if (schedule.isAllDay) "종일" else "${formatTime(schedule.startAt)} - ${formatTime(schedule.endAt)}"

/** e.g. "2시간 후", "15분 후", "곧 시작", "진행중", "종료됨" */
fun remainingLabel(schedule: Schedule, nowMillis: Long = System.currentTimeMillis()): String {
    if (nowMillis >= schedule.endAt) return "종료됨"
    if (nowMillis >= schedule.startAt) return "진행중"

    val remaining = Duration.ofMillis(schedule.startAt - nowMillis)
    val days = remaining.toDays()
    val hours = remaining.toHours()
    val minutes = remaining.toMinutes()

    return when {
        minutes < 1 -> "곧 시작"
        days >= 1 -> "${days}일 후"
        hours >= 1 -> "${hours}시간 후"
        else -> "${minutes}분 후"
    }
}
