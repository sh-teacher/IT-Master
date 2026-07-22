package com.baefamily.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baefamily.schedule.data.model.FamilyRole
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.data.repository.AuthRepository
import com.baefamily.schedule.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object LoggedOut : AuthUiState
    data class NeedsRoleSelection(val uid: String) : AuthUiState
    data class LoggedIn(val profile: UserProfile) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _takenRoles = MutableStateFlow<Set<FamilyRole>>(emptySet())
    val takenRoles: StateFlow<Set<FamilyRole>> = _takenRoles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState().collect { user ->
                if (user == null) {
                    _uiState.value = AuthUiState.LoggedOut
                } else {
                    refreshProfile(user.uid)
                }
            }
        }
    }

    private suspend fun refreshProfile(uid: String) {
        val profile = userRepository.getUserProfile(uid)
        _uiState.value = if (profile == null) {
            AuthUiState.NeedsRoleSelection(uid)
        } else {
            AuthUiState.LoggedIn(profile)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            authRepository.signIn(email, password).onFailure {
                _errorMessage.value = it.message ?: "로그인에 실패했습니다."
            }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            authRepository.signUp(email, password).onFailure {
                _errorMessage.value = it.message ?: "회원가입에 실패했습니다."
            }
            _isLoading.value = false
        }
    }

    fun loadTakenRoles() {
        viewModelScope.launch {
            _takenRoles.value = userRepository.getTakenRoles()
        }
    }

    fun completeRoleSelection(uid: String, name: String, role: FamilyRole) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            userRepository.createUserProfile(uid, name, role)
                .onSuccess { refreshProfile(uid) }
                .onFailure { _errorMessage.value = it.message ?: "프로필 생성에 실패했습니다." }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
