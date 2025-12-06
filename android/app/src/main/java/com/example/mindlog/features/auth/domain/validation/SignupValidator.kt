package com.example.mindlog.features.auth.domain.validation

import java.time.YearMonth

class SignupValidator {
    fun validate(
        username: String,
        loginId: String,
        password: String,
        confirmPassword: String,
        gender: String?,
        birthYear: Int?,
        birthMonth: Int?,
        birthDay: Int?
    ): SignupValidationResult {
        if (username.isBlank() || loginId.isBlank() || password.isBlank()) {
            return SignupValidationResult(
                isValid = false,
                errorMessage = "모든 필드를 입력해주세요."
            )
        }
        if (gender == null) {
            return SignupValidationResult(false, "성별을 선택해주세요.")
        }
        if (birthYear == null || birthMonth == null || birthDay == null) {
            return SignupValidationResult(false, "생년월일을 입력해주세요.")
        }
        if (!isValidDate(birthYear, birthMonth, birthDay)) {
            return SignupValidationResult(false, "유효하지 않은 생년월일입니다.")
        }
        if (!isValidLoginId(loginId)) {
            return SignupValidationResult(false, "로그인 아이디는 영어 대소문자와 숫자로만 입력해주세요.")
        }
        if (!isValidPassword(password)) {
            return SignupValidationResult(false, "비밀번호는 특수문자, 영어, 숫자 중 2가지 이상을 포함하고 8자 이상이어야 합니다.")
        }
        if (password != confirmPassword) {
            return SignupValidationResult(false, "비밀번호가 일치하지 않습니다.")
        }
        return SignupValidationResult(true, null)
    }

    private fun isValidDate(y: Int, m: Int, d: Int): Boolean {
        return try {
            YearMonth.of(y, m).atDay(d)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidLoginId(id: String): Boolean {
        val regex = "^[A-Za-z0-9]+$".toRegex()
        return regex.matches(id)
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false

        var hasLetter = false
        var hasDigit = false
        var hasSpecial = false

        password.forEach { ch ->
            when {
                ch.isLetter() -> hasLetter = true
                ch.isDigit() -> hasDigit = true
                !ch.isLetterOrDigit() -> hasSpecial = true
            }
        }

        // 특수문자 / 영어 / 숫자 중 2가지 이상 포함
        val categoryCount = listOf(hasLetter, hasDigit, hasSpecial).count { it }
        return categoryCount >= 2
    }
}