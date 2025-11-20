package com.example.mindlog.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _logoutEvent = MutableSharedFlow<Result<Unit>>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            try {
                if (authRepository.logout()) {
                    _logoutEvent.emit(Result.success(Unit))
                } else {
                    _logoutEvent.emit(Result.failure(Exception("로그아웃에 실패했습니다.")))
                }
            } catch (e: Exception) {
                _logoutEvent.emit(Result.failure(e))
            }
        }
    }
}
