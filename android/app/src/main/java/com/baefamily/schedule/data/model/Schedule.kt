package com.baefamily.schedule.data.model

/**
 * startAt/endAt are epoch millis (UTC instant), always interpreted in the device's default zone.
 * recurrenceGroupId is shared by every occurrence generated from the same "매주 반복" registration,
 * and is what lets us bulk-delete "this and following" occurrences with a single range query.
 */
data class Schedule(
    val id: String = "",
    val ownerUid: String = "",
    val role: FamilyRole = FamilyRole.DAD,
    val title: String = "",
    val memo: String = "",
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val isAllDay: Boolean = false,
    val recurrenceGroupId: String? = null,
    val leadMinutes: Int = 10,
    val vibrate: Boolean = true
)
