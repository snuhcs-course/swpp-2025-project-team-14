package com.example.mindlog.features.auth.domain.validation

object SignupErrorMapper {
    fun map(message: String?): String? {
        if (message.isNullOrBlank()) return null
        return when {
            message.contains("password", ignoreCase = true) ->
                "조금 더 복잡한 비밀번호를 사용해주세요."
            message.contains("login ID", ignoreCase = true) ||
                    message.contains("loginId", ignoreCase = true) ||
                    message.contains("login_id", ignoreCase = true) ->
                "동일한 로그인 아이디가 존재합니다. 다른 아이디를 사용해주세요."
            message.contains("username", ignoreCase = true) ->
                "사용자 이름이 유효하지 않습니다. 옯바른 이름을 입력해주세요."
            else -> message
        }
    }
}