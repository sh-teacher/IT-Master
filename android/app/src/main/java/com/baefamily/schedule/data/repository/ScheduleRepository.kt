package com.baefamily.schedule.data.repository

import com.baefamily.schedule.data.firebase.FirestoreRefs
import com.baefamily.schedule.data.model.FamilyRole
import com.baefamily.schedule.data.model.RecurrenceInput
import com.baefamily.schedule.data.model.Schedule
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class ScheduleRepository(private val db: FirebaseFirestore = FirestoreRefs.db()) {

    private val schedulesRef = db.collection(FirestoreRefs.SCHEDULES)
    private val zone: ZoneId = ZoneId.systemDefault()

    /** [fromMillis, toMillisExclusive) range query, shared by calendar month view and family timeline. */
    fun observeRange(fromMillis: Long, toMillisExclusive: Long): Flow<List<Schedule>> = callbackFlow {
        val query = schedulesRef
            .whereGreaterThanOrEqualTo("startAt", fromMillis)
            .whereLessThan("startAt", toMillisExclusive)
            .orderBy("startAt", Query.Direction.ASCENDING)
        val registration = query.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents?.mapNotNull { it.toSchedule() } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    suspend fun getById(scheduleId: String): Schedule? =
        schedulesRef.document(scheduleId).get().await().toSchedule()

    /** One-shot (non-listening) fetch, used by BootReceiver to reschedule alarms after a reboot. */
    suspend fun getRangeOnce(fromMillis: Long, toMillisExclusive: Long): List<Schedule> {
        val snapshot = schedulesRef
            .whereGreaterThanOrEqualTo("startAt", fromMillis)
            .whereLessThan("startAt", toMillisExclusive)
            .orderBy("startAt", Query.Direction.ASCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { it.toSchedule() }
    }

    suspend fun addSingleSchedule(schedule: Schedule): Result<Schedule> = runCatching {
        val ref = schedulesRef.document()
        val withId = schedule.copy(id = ref.id)
        ref.set(withId.toMap()).await()
        withId
    }

    /** Materializes one document per matching weekday between the base start date and [recurrence.untilDate] (inclusive, capped). */
    suspend fun addRecurringSchedule(schedule: Schedule, recurrence: RecurrenceInput): Result<List<Schedule>> = runCatching {
        val groupId = UUID.randomUUID().toString()
        val startDateTime = Instant.ofEpochMilli(schedule.startAt).atZone(zone).toLocalDateTime()
        val duration = Duration.ofMillis(schedule.endAt - schedule.startAt)
        val firstDate = startDateTime.toLocalDate()
        val lastDate = minOf(recurrence.untilDate, firstDate.plusDays(RecurrenceInput.MAX_RANGE_DAYS))

        val occurrences = mutableListOf<Schedule>()
        var date = firstDate
        while (!date.isAfter(lastDate)) {
            if (recurrence.daysOfWeek.contains(date.dayOfWeek)) {
                val occurrenceStart = date.atTime(startDateTime.toLocalTime())
                val startMillis = occurrenceStart.atZone(zone).toInstant().toEpochMilli()
                val endMillis = startMillis + duration.toMillis()
                occurrences += schedule.copy(startAt = startMillis, endAt = endMillis, recurrenceGroupId = groupId)
            }
            date = date.plusDays(1)
        }

        val saved = mutableListOf<Schedule>()
        occurrences.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { occurrence ->
                val ref = schedulesRef.document()
                val withId = occurrence.copy(id = ref.id)
                batch.set(ref, withId.toMap())
                saved += withId
            }
            batch.commit().await()
        }
        saved
    }

    suspend fun updateSchedule(schedule: Schedule): Result<Unit> = runCatching {
        schedulesRef.document(schedule.id).set(schedule.toMap()).await()
    }

    suspend fun deleteSingle(scheduleId: String): Result<Unit> = runCatching {
        schedulesRef.document(scheduleId).delete().await()
    }

    /** Returns the deleted document ids so the caller can cancel their local alarms. */
    suspend fun deleteThisAndFollowing(recurrenceGroupId: String, fromMillis: Long): Result<List<String>> = runCatching {
        val snapshot = schedulesRef
            .whereEqualTo("recurrenceGroupId", recurrenceGroupId)
            .whereGreaterThanOrEqualTo("startAt", fromMillis)
            .get().await()
        deleteDocuments(snapshot.documents)
    }

    suspend fun deleteEntireSeries(recurrenceGroupId: String): Result<List<String>> = runCatching {
        val snapshot = schedulesRef
            .whereEqualTo("recurrenceGroupId", recurrenceGroupId)
            .get().await()
        deleteDocuments(snapshot.documents)
    }

    private suspend fun deleteDocuments(documents: List<DocumentSnapshot>): List<String> {
        documents.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        return documents.map { it.id }
    }
}

private fun Schedule.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "ownerUid" to ownerUid,
    "role" to role.id,
    "title" to title,
    "memo" to memo,
    "startAt" to startAt,
    "endAt" to endAt,
    "isAllDay" to isAllDay,
    "recurrenceGroupId" to recurrenceGroupId,
    "leadMinutes" to leadMinutes,
    "vibrate" to vibrate
)

private fun DocumentSnapshot.toSchedule(): Schedule? {
    if (!exists()) return null
    val roleId = getString("role") ?: return null
    val role = FamilyRole.fromId(roleId) ?: return null
    return Schedule(
        id = id,
        ownerUid = getString("ownerUid") ?: "",
        role = role,
        title = getString("title") ?: "",
        memo = getString("memo") ?: "",
        startAt = getLong("startAt") ?: 0L,
        endAt = getLong("endAt") ?: 0L,
        isAllDay = getBoolean("isAllDay") ?: false,
        recurrenceGroupId = getString("recurrenceGroupId"),
        leadMinutes = (getLong("leadMinutes") ?: 10L).toInt(),
        vibrate = getBoolean("vibrate") ?: true
    )
}
