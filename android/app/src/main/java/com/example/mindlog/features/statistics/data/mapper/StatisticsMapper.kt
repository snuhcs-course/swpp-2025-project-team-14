package com.example.mindlog.features.statistics.data.mapper

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.statistics.data.dto.EmotionRateItem
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionEvent
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import javax.inject.Inject

class StatisticsMapper @Inject constructor() {

    private val EMOTION_PAIRS = listOf(
        Emotion.HAPPY to Emotion.SAD,
        Emotion.CALM to Emotion.ANXIOUS,
        Emotion.ANNOYED to Emotion.SATISFIED,
        Emotion.INTERESTED to Emotion.BORED,
        Emotion.LETHARGIC to Emotion.ENERGETIC,
    )

    private fun parseEmotion(value: String?): Emotion =
        value?.let { Emotion.fromApi(it) } ?: Emotion.CALM

    fun toEmotionRate(dto: EmotionRateItem) = EmotionRate(
        emotion = Emotion.fromApi(dto.emotion) ?: Emotion.CALM,
        count = dto.count,
        percentage = dto.percentage / 100f
    )

    fun toJournalStatistics(journals: List<JournalItemResponse>): JournalStatistics {
        // 감정 트렌드 (날짜별 감정 intensity 평균)
        val trendByEmotion = mutableMapOf<Emotion, MutableList<Pair<String, Int>>>()
        for (journal in journals) {
            val date = journal.createdAt.take(10) // "2025-11-09T..." → "2025-11-09"

            val intensityByEmotion: Map<Emotion, Int> =
                journal.emotions
                    .mapNotNull { er ->
                        val emo = Emotion.fromApi(er.emotion) ?: return@mapNotNull null
                        emo to er.intensity
                    }
                    .toMap()

            for ((e1, e2) in EMOTION_PAIRS) {
                val i1 = intensityByEmotion[e1]
                val i2 = intensityByEmotion[e2]

                if (i1 == null && i2 == null) continue
                if ((i1 ?: 0) == 0 && (i2 ?: 0) == 0) {
                    continue
                }
                if (i1 != null) {
                    trendByEmotion
                        .getOrPut(e1) { mutableListOf() }
                        .add(date to i1)
                }
                if (i2 != null) {
                    trendByEmotion
                        .getOrPut(e2) { mutableListOf() }
                        .add(date to i2)
                }
            }
        }

        val emotionTrends = trendByEmotion.map { (emotion: Emotion, entries) ->
            val grouped = entries.groupBy { it.first }
            val dailyAvg = grouped.mapValues { (_, list) ->
                list.map { it.second }.average().toInt()
            }
            // 날짜 순으로 intensity 정렬
            EmotionTrend(
                emotion = emotion,
                trend = dailyAvg.toSortedMap().values.toList()
            )
        }

        // 감정 이벤트 (강한 intensity를 가진 일기의 제목 or 요약)
        val emotionEvents = journals.flatMap { journal ->
            (journal.keywords ?: emptyList())
                .asSequence()
                .filter { it.weight >= 0.5f } // 키워드 신뢰도 임계값
                .mapNotNull { kw ->
                    val emo = Emotion.fromApi(kw.emotion) ?: return@mapNotNull null
                    val text = (kw.summary?.takeIf { it.isNotBlank() } ?: kw.keyword).trim()
                    if (text.isBlank()) null else emo to text
                }
                .toList()
        }
            .groupBy({ it.first }, { it.second })
            .map { (emotion, texts) ->
                // 같은/유사 텍스트 중복 제거 후 최대 5개까지만 사용
                val distinctTexts = texts.distinct().take(5)
                EmotionEvent(emotion = emotion, events = distinctTexts)
            }

        // 키워드 빈도 (weight 기반 가중치 반영)
        val keywordCount = mutableMapOf<String, Int>()
        for (journal in journals) {
            journal.keywords?.forEach { kw ->
                // weight 기반 가중치: 0.5~0.65 → +1, 0.65~0.8 → +2, 0.8~1.0 → +3
                val weightContribution = when (kw.weight) {
                    in 0.5f..0.65f -> 1
                    in 0.65f.. 0.8f -> 2
                    in 0.8f..1.0f -> 3
                    else -> 0
                }

                if (weightContribution > 0) {
                    val key = kw.keyword
                    keywordCount[key] = (keywordCount[key] ?: 0) + weightContribution
                }
            }
        }

        val journalKeywords = keywordCount.entries
            .sortedByDescending { it.value }
            .take(50)
            .map { (keyword, count) ->
                JournalKeyword(
                    keyword = keyword,
                    count = count
                )
            }

        // 최종 조합
        return JournalStatistics(
            EmotionTrends = emotionTrends,
            EmotionEvents = emotionEvents,
            JournalKeywords = journalKeywords
        )
    }
}