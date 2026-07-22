package com.baefamily.schedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.baefamily.schedule.data.model.Schedule

/**
 * Schedules are personal reminders: each occurrence is only alarmed on the owner's own device
 * (see ScheduleViewModel, which only calls this when schedule.ownerUid == the signed-in user).
 */
object AlarmScheduler {
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_VIBRATE = "extra_vibrate"

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun schedule(context: Context, schedule: Schedule) {
        if (schedule.leadMinutes < 0) return
        val triggerAt = schedule.startAt - schedule.leadMinutes * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return

        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_TITLE, schedule.title)
            putExtra(EXTRA_VIBRATE, schedule.vibrate)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(schedule.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context, scheduleId: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(scheduleId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAll(context: Context, scheduleIds: List<String>) {
        scheduleIds.forEach { cancel(context, it) }
    }

    private fun requestCode(scheduleId: String): Int = scheduleId.hashCode()
}
