package com.baefamily.schedule.data.model

import java.time.DayOfWeek
import java.time.LocalDate

/** UI-side input for generating recurring occurrences; not persisted as its own document. */
data class RecurrenceInput(
    val daysOfWeek: Set<DayOfWeek>,
    val untilDate: LocalDate
) {
    companion object {
        const val MAX_RANGE_DAYS = 366L
    }
}
