package com.example.mindlog.features.auth.presentation.signup


import com.example.mindlog.core.domain.Result
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate


@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {
    data class UiState(
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val errorMessage: String? = null
    )
    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> get() = _state

    fun signup(
        loginId: String,
        password: String,
        username: String,
        gender: String,
        birthDate: LocalDate
    ) {
        viewModelScope.launch(dispatcher.io) {
            when (val result = signupUseCase(loginId, password, username, gender, birthDate)) {
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
                            errorMessage = result.message ?: "회원가입 중 오류 발생"
                        )
                    )
                }
            }
        }
    }
}