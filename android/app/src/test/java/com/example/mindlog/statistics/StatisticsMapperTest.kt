package com.example.mindlog.statistics

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.KeywordResponse
import com.example.mindlog.features.journal.data.dto.EmotionResponse
import com.example.mindlog.features.statistics.data.dto.EmotionRateItem
import com.example.mindlog.features.statistics.data.mapper.StatisticsMapper
import com.example.mindlog.features.statistics.domain.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatisticsMapperTest {
    private lateinit var mapper: StatisticsMapper

    @Before
    fun setUp() {
        mapper = StatisticsMapper()
    }

    @Test
    fun `toEmotionRate normalizes percent to fraction and maps emotion`() {
        // given
        val dto = EmotionRateItem(
            emotion = "happy",
            count = 10,
            percentage = 10.86f // percent from backend
        )

        // when
        val res = mapper.toEmotionRate(dto)

        // then
        assertEquals(Emotion.HAPPY, res.emotion)
        assertEquals(10, res.count)
        assertEquals(0.1086f, res.percentage, 0.0001f)
    }

    @Test
    fun `toEmotionRate falls back to CALM on unknown emotion`() {
        val dto = EmotionRateItem(
            emotion = "unknown_x",
            count = 1,
            percentage = 1.0f
        )
        val res = mapper.toEmotionRate(dto)
        assertEquals(Emotion.CALM, res.emotion)
    }

    @Test
    fun `toJournalStatistics builds trends averaged by day and sorted by date`() {
        // given two journals: same day with different intensities for HAPPY and next day one value
        val j1 = journal(
            createdAt = "2025-11-01T10:00:00Z",
            emotions = listOf(EmotionResponse(emotion = "happy", intensity = 60))
        )
        val j2 = journal(
            createdAt = "2025-11-01T20:00:00Z",
            emotions = listOf(EmotionResponse(emotion = "happy", intensity = 80))
        )
        val j3 = journal(
            createdAt = "2025-11-02T09:00:00Z",
            emotions = listOf(EmotionResponse(emotion = "happy", intensity = 40))
        )

        // when
        val stats = mapper.toJournalStatistics(listOf(j1, j2, j3))

        // then: average(60,80)=70 then 40 → trend [70,40]
        val trend = stats.EmotionTrends.first { it.emotion == Emotion.HAPPY }
        assertEquals(listOf(70, 40), trend.trend)
    }

    @Test
    fun `toJournalStatistics builds events from keywords with weight filter and summary fallback`() {
        val j1 = journal(
            createdAt = "2025-11-03T12:00:00Z",
            keywords = listOf(
                // 포함: 높은 weight, summary 사용
                KeywordResponse(
                    keyword = "운동",
                    emotion = "energetic",
                    summary = "퇴근 후 운동을 다시 시작했다.",
                    weight = 0.9f
                ),
                // 포함: summary 비어있으면 keyword 텍스트 사용
                KeywordResponse(
                    keyword = "퇴근",
                    emotion = "energetic",
                    summary = "",
                    weight = 0.7f
                ),
                // 현재 구현상 포함되는 케이스 (weight 0.6)
                KeywordResponse(
                    keyword = "게임",
                    emotion = "energetic",
                    summary = "밤에 잠깐 게임",
                    weight = 0.6f
                )
            )
        )
        val stats = mapper.toJournalStatistics(listOf(j1))

        val energetic = stats.EmotionEvents.first { it.emotion == Emotion.ENERGETIC }

        // 이제 3개를 기대
        assertEquals(3, energetic.events.size)
        // 내용은 모두 포함되어야 함 (순서는 상관 없음)
        assertTrue(energetic.events.contains("퇴근 후 운동을 다시 시작했다."))
        assertTrue(energetic.events.contains("퇴근"))
        assertTrue(energetic.events.contains("밤에 잠깐 게임"))
    }

    @Test
    fun `toJournalStatistics counts keywords frequency top10 with weight contribution`() {
        val j1 = journal(
            createdAt = "2025-11-01T00:00:00Z",
            keywords = listOf(
                KeywordResponse(keyword = "운동", emotion = "energetic", summary = "운동하는", weight = 0.9f),
                KeywordResponse(keyword = "독서", emotion = "calm", summary = "독서하는", weight = 0.8f)
            )
        )
        val j2 = journal(
            createdAt = "2025-11-02T00:00:00Z",
            keywords = listOf(
                KeywordResponse(keyword = "운동", emotion = "energetic", summary = "운동하는", weight = 0.7f),
                KeywordResponse(keyword = "요리", emotion = "happy", summary = "독서하는", weight = 0.7f)
            )
        )
        val stats = mapper.toJournalStatistics(listOf(j1, j2))

        // keywordCount uses weight-based contribution: 0.5~0.65 -> +1, 0.65~0.8 -> +2, 0.8~1.0 -> +3
        val map = stats.JournalKeywords.associate { it.keyword to it.count }
        assertEquals(5, map["운동"])
        assertEquals(2, map["독서"])
        assertEquals(2, map["요리"])
    }

    // ---- helpers ----
    private fun journal(
        id: Int = 0,
        title: String = "테스트 제목",
        content: String = "테스트 내용",
        emotions: List<EmotionResponse> = emptyList(),
        gratitude: String = "테스트 감사",
        imageS3Keys: String = "test_image_key",
        createdAt: String,
        keywords: List<KeywordResponse> = emptyList()
    ): JournalItemResponse = JournalItemResponse(
        id = id,
        title = title,
        content = content,
        emotions = emotions,
        gratitude = gratitude,
        imageS3Keys = imageS3Keys,
        createdAt = createdAt,
        keywords = keywords
    )
}