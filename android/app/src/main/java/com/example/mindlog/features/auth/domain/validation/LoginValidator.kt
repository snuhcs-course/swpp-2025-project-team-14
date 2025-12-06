package com.example.mindlog.features.auth.domain.validation

class LoginValidator {
    fun validate(loginId: String, password: String): LoginValidationResult {
        val isIdEmpty = loginId.isBlank()
        val isPasswordEmpty = password.isBlank()
        val msg = when {
            isIdEmpty && isPasswordEmpty -> "아이디와 비밀번호를 입력해주세요"
            isIdEmpty -> "아이디를 입력해주세요"
            isPasswordEmpty -> "비밀번호를 입력해주세요"
            else -> null
        }
        return LoginValidationResult(isValid = msg == null, errorMessage = msg)
    }
}