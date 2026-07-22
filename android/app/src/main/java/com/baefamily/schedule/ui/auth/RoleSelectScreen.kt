package com.baefamily.schedule.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.baefamily.schedule.data.model.FamilyRole
import com.baefamily.schedule.ui.theme.toColor
import com.baefamily.schedule.viewmodel.AuthViewModel

@Composable
fun RoleSelectScreen(
    uid: String,
    viewModel: AuthViewModel
) {
    val takenRoles by viewModel.takenRoles.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<FamilyRole?>(null) }

    LaunchedEffect(Unit) { viewModel.loadTakenRoles() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("당신은 누구인가요?", style = MaterialTheme.typography.headlineSmall)
        Text("가족 중 한 명을 선택하고 이름을 입력해주세요", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val rows = FamilyRole.entries.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            rows.forEach { rowRoles ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    rowRoles.forEach { role ->
                        RoleCard(
                            role = role,
                            taken = role in takenRoles,
                            selected = role == selectedRole,
                            onClick = { selectedRole = role },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { viewModel.completeRoleSelection(uid, name.trim(), selectedRole!!) },
            enabled = !isLoading && name.isNotBlank() && selectedRole != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("시작하기")
            }
        }
    }
}

@Composable
private fun RoleCard(
    role: FamilyRole,
    taken: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = role.toColor()
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(enabled = !taken, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) roleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (taken) Color.LightGray else if (selected) roleColor else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(role.emoji, style = MaterialTheme.typography.displaySmall)
            Text(
                role.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (taken) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
            if (taken) {
                Text("이미 등록됨", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
