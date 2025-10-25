package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * JournalListResponse 및 GetJournalById API의 응답 본문을 나타내는 데이터 클래스
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

    @SerializedName("gratitude")
    val gratitude: String,

    // ... image_s3_keys, summary 등 다른 필드들도 필요 시 여기에 추가할 수 있습니다.

    @SerializedName("created_at")
    val createdAt: String
)
