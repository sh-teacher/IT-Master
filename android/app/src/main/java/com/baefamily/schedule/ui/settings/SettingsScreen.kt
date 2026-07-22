package com.baefamily.schedule.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baefamily.schedule.data.model.NotificationSettings
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.notification.AlarmScheduler
import com.baefamily.schedule.ui.common.LeadMinutesDropdown
import com.baefamily.schedule.ui.theme.toColor
import com.baefamily.schedule.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentUser: UserProfile,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current

    var enabled by remember(currentUser.uid) { mutableStateOf(currentUser.notificationSettings.enabled) }
    var vibrate by remember(currentUser.uid) { mutableStateOf(currentUser.notificationSettings.vibrate) }
    var leadMinutes by remember(currentUser.uid) { mutableStateOf(currentUser.notificationSettings.defaultLeadMinutes) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    fun persist(newEnabled: Boolean = enabled, newVibrate: Boolean = vibrate, newLead: Int = leadMinutes) {
        viewModel.updateNotificationSettings(currentUser.uid, NotificationSettings(newEnabled, newVibrate, newLead))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("내 설정") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(currentUser.role.toColor().copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentUser.role.emoji, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(currentUser.name, style = MaterialTheme.typography.titleLarge)
                    Text(currentUser.role.displayName, color = currentUser.role.toColor())
                }
            }

            HorizontalDivider()

            Text("알림 설정", style = MaterialTheme.typography.titleSmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("일정 알림 사용", modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it; persist(newEnabled = it) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("진동 사용", modifier = Modifier.weight(1f))
                Switch(
                    checked = vibrate,
                    enabled = enabled,
                    onCheckedChange = { vibrate = it; persist(newVibrate = it) }
                )
            }
            LeadMinutesDropdown(
                selected = leadMinutes,
                enabled = enabled,
                onSelect = { leadMinutes = it; persist(newLead = it) },
                label = "기본 사전 알림"
            )
            Text(
                "새 일정을 등록할 때 기본으로 적용되는 알림 시간이에요. 일정마다 개별로 바꿀 수도 있어요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!AlarmScheduler.canScheduleExactAlarms(context)) {
                OutlinedButton(
                    onClick = { context.startActivity(AlarmScheduler.exactAlarmSettingsIntent(context)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("정확한 알람 권한 허용하기")
                }
            }

            HorizontalDivider()

            OutlinedButton(onClick = { showSignOutConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("로그아웃")
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("로그아웃") },
            text = { Text("로그아웃 하시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    viewModel.signOut()
                    onSignedOut()
                }) { Text("로그아웃") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("취소") }
            }
        )
    }
}
