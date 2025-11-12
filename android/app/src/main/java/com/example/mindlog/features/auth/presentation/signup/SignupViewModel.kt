package com.example.mindlog.features.auth.presentation.signup

import com.example.mindlog.core.common.Result
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.sql.Date
import javax.inject.Inject
import java.time.LocalDate


@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase
) : ViewModel() {

    val signupResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    fun signup(
        loginId: String,
        password: String,
        username: String,
        gender: String,
        birthDate: LocalDate
    ) {
        viewModelScope.launch {
            when (val result = signupUseCase(loginId, password, username, gender, birthDate)) {
                is Result.Success -> {
                    signupResult.value = result.data   // true / false 값 그대로 전달
                    errorMessage.value = null
                }

                is Result.Error -> {
                    signupResult.value = false
                    errorMessage.value =
                        result.message ?: "회원가입 중 오류가 발생했습니다 (${result.code ?: "unknown"})"
                }
            }
        }
    }
}
