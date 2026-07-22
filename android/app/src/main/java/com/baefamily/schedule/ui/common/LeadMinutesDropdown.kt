package com.baefamily.schedule.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

val LEAD_MINUTES_OPTIONS = listOf(
    0 to "정시 알림", 5 to "5분 전", 10 to "10분 전", 15 to "15분 전",
    30 to "30분 전", 60 to "1시간 전", 120 to "2시간 전", 1440 to "1일 전"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadMinutesDropdown(
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    label: String = "사전 알림"
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = LEAD_MINUTES_OPTIONS.find { it.first == selected }?.second ?: "${selected}분 전"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LEAD_MINUTES_OPTIONS.forEach { (minutes, label2) ->
                DropdownMenuItem(
                    text = { Text(label2) },
                    onClick = { onSelect(minutes); expanded = false }
                )
            }
        }
    }
}
