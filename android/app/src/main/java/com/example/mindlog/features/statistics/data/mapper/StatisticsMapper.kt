package com.example.mindlog.features.statistics.data.mapper

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.statistics.data.dto.EmotionRateResponse
import com.example.mindlog.features.statistics.data.dto.EmotionRatesResponse
import com.example.mindlog.features.statistics.domain.model.EmotionEvent
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import javax.inject.Inject

class StatisticsMapper @Inject constructor() {
    fun toEmotionRate(dto: EmotionRateResponse) = EmotionRate(
        emotion =  dto.emotion,
        count = dto.count,
        percentage = dto.percentage
    )

    fun toJournalStatistics(journals: List<JournalItemResponse>): JournalStatistics {
        // 감정 트렌드 (날짜별 감정 intensity 평균)
        val trendByEmotion = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        for (journal in journals) {
            val date = journal.createdAt.take(10) // "2025-11-09T..." → "2025-11-09"
            for (emotion in journal.emotions) {
                trendByEmotion
                    .getOrPut(emotion.emotion) { mutableListOf() }
                    .add(date to emotion.intensity)
            }
        }

        val emotionTrends = trendByEmotion.map { (emotion, entries) ->
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
            journal.emotions
                .filter { it.intensity >= 80 } // intensity 기준값(조정 가능)
                .map {
                    it.emotion to journal.title
                }
        }.groupBy({ it.first }, { it.second })
            .map { (emotion, titles) ->
                EmotionEvent(emotion = emotion, events = titles.distinct().take(5))
            }

        // 키워드 빈도 (모든 일기의 keywords)
        val keywordCount = mutableMapOf<String, Int>()
        for (journal in journals) {
            journal.keywords?.forEach {
                keywordCount[it.keyword] = (keywordCount[it.keyword] ?: 0) + 1
            }
        }

        val journalKeywords = keywordCount.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { (k, v) -> JournalKeyword(keyword = k, count = v) }

        // 최종 조합
        return JournalStatistics(
            EmotionTrends = emotionTrends,
            EmotionEvents = emotionEvents,
            JournalKeywords = journalKeywords
        )
    }
}