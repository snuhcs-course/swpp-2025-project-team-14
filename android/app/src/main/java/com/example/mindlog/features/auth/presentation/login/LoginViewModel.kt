package com.example.mindlog.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.mindlog.core.domain.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val dispatcher: DispatcherProvider     // ✅ DispatcherProvider 주입
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val errorMessage: String? = null
    )

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> get() = _state

    fun login(loginId: String, password: String) {
        viewModelScope.launch(dispatcher.io) {    // ✅ IO 디스패처에서 API 호출
            when (val result = loginUseCase(loginId, password)) {
                is Result.Success -> {
                    _state.postValue(
                        UiState(
                            isLoading = false,
                            isSuccess = result.data,
                            errorMessage = null
                        )
                    )
                }
                is Result.Error -> {
                    _state.postValue(
                        UiState(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = result.message ?: "로그인 중 오류가 발생했습니다 (${result.code ?: "unknown"})"
                        )
                    )
                }
            }
        }
    }
}