package com.baefamily.schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baefamily.schedule.data.repository.AuthRepository
import com.baefamily.schedule.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** AlarmManager alarms are cleared on reboot; re-derive them from Firestore for the signed-in owner. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = AuthRepository().currentUser?.uid ?: return@launch
                val now = System.currentTimeMillis()
                val horizon = now + TimeUnit.DAYS.toMillis(60)
                ScheduleRepository().getRangeOnce(now, horizon)
                    .filter { it.ownerUid == uid }
                    .forEach { AlarmScheduler.schedule(appContext, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
