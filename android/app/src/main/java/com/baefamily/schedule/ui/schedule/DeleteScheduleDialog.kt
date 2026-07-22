package com.baefamily.schedule.ui.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun DeleteScheduleDialog(
    isRecurring: Boolean,
    onDismiss: () -> Unit,
    onDeleteSingle: () -> Unit,
    onDeleteFollowing: () -> Unit,
    onDeleteAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 삭제") },
        text = {
            Text(if (isRecurring) "반복 일정이에요. 어떻게 삭제할까요?" else "이 일정을 삭제할까요?")
        },
        confirmButton = {
            if (isRecurring) {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onDeleteSingle) { Text("이 일정만 삭제") }
                    TextButton(onClick = onDeleteFollowing) { Text("이 일정부터 이후 전체 삭제") }
                    TextButton(onClick = onDeleteAll) { Text("전체 반복 일정 삭제", color = MaterialTheme.colorScheme.error) }
                }
            } else {
                TextButton(onClick = onDeleteSingle) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
