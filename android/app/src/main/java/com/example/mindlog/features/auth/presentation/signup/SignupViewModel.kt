package com.example.mindlog.features.auth.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import kotlinx.coroutines.launch

class SignupViewModel(private val signupUseCase: SignupUseCase): ViewModel() {
    val signupResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    fun signup(loginId: String, password: String, username: String) {
        viewModelScope.launch {
            try {
                val token = signupUseCase(loginId, password, username)
                signupResult.value = token != null
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }
}
