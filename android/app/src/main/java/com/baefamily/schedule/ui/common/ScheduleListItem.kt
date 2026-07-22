package com.baefamily.schedule.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.ui.theme.toColor
import com.baefamily.schedule.util.remainingLabel
import com.baefamily.schedule.util.timeRangeLabel

@Composable
fun ScheduleListItem(
    schedule: Schedule,
    ownerName: String,
    modifier: Modifier = Modifier,
    showRemaining: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(schedule.role.toColor())
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(schedule.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${schedule.role.emoji} $ownerName · ${timeRangeLabel(schedule)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showRemaining) {
            Text(
                remainingLabel(schedule),
                style = MaterialTheme.typography.labelMedium,
                color = schedule.role.toColor()
            )
            Spacer(Modifier.width(6.dp))
        }

        if (schedule.recurrenceGroupId != null) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = "반복 일정",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
