package com.example.mindlog.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    val loginResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    fun login(loginId: String, password: String) {
        viewModelScope.launch {
            isLoading.value = true

            when (val result = loginUseCase(loginId, password)) {
                is Result.Success -> {
                    loginResult.value = result.data
                    errorMessage.value = null
                }

                is Result.Error -> {
                    loginResult.value = false
                    errorMessage.value =
                        result.message ?: "로그인 중 오류가 발생했습니다 (${result.code ?: "unknown"})"
                }
            }

            isLoading.value = false
        }
    }
}