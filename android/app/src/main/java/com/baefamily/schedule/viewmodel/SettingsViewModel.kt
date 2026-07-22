package com.baefamily.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baefamily.schedule.data.model.NotificationSettings
import com.baefamily.schedule.data.repository.AuthRepository
import com.baefamily.schedule.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun updateNotificationSettings(uid: String, settings: NotificationSettings) {
        viewModelScope.launch {
            _isSaving.value = true
            userRepository.updateNotificationSettings(uid, settings)
            _isSaving.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
