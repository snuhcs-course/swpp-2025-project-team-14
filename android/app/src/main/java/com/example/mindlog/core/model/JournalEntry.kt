package com.example.mindlog.core.model

// 1. 날짜 필드 추가
import java.util.Date

data class JournalEntry(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: Date // 2. 날짜 필드 추가 (또는 String 타입으로 관리해도 됨)
)
