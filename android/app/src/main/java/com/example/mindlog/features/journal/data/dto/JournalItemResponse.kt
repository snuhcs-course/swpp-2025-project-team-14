package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * JournalListResponse 내의 개별 일기 항목을 나타내는 데이터 클래스
 */
data class JournalItemResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("emotions")
    val emotions: List<EmotionResponse>,

    // ... 다른 필드들 ...

    @SerializedName("created_at")
    val createdAt: String
)
