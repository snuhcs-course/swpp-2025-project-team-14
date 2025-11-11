package com.example.mindlog.features.auth.presentation.signup

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
): ViewModel() {

    val signupResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    // 새 메서드: UI에서 넘겨주는 성별/생년월일까지 받기
    fun signup(
        loginId: String,
        password: String,
        username: String,
        gender: String,
        birthDate: LocalDate
    ) {
        viewModelScope.launch {
            try {
                // TODO: 백엔드가 준비되면 gender/birth를 request body에 포함해 전달
                val result = signupUseCase(loginId, password, username, gender, birthDate)
                signupResult.value = result.isSuccess
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }
}
