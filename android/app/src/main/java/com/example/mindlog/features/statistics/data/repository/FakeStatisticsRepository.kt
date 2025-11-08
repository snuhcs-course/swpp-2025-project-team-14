package com.example.mindlog.features.statistics.data.repository

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.respository.StatisticsRepository
import javax.inject.Inject

class FakeStatisticsRepository @Inject constructor() : StatisticsRepository {

    override suspend fun getEmotionRatio(): Result<List<EmotionRate>> {
        val ratios = listOf(
            EmotionRate("행복", 0.18f),
            EmotionRate("슬픔", 0.10f),
            EmotionRate("자신감", 0.08f),
            EmotionRate("불안", 0.05f),
            EmotionRate("편안", 0.09f),
            EmotionRate("짜증", 0.11f),
            EmotionRate("흥미", 0.10f),
            EmotionRate("지루함", 0.11f),
            EmotionRate("활력", 0.09f),
            EmotionRate("무기력", 0.09f),
        )
        return Result.Success(ratios)
    }

    override suspend fun getEmotionTrend(): Result<List<EmotionTrend>> {
        val trends = listOf(
            EmotionTrend("행복", listOf(0.30f, 0.45f, 0.50f, 0.55f, 0.62f, 0.58f, 0.66f)),
            EmotionTrend("슬픔", listOf(0.40f, 0.35f, 0.25f, 0.30f, 0.22f, 0.20f, 0.18f)),
            EmotionTrend("불안", listOf(0.20f, 0.15f, 0.10f, 0.12f, 0.08f, 0.09f, 0.07f)),
            EmotionTrend("무기력", listOf(0.50f, 0.48f, 0.42f, 0.40f, 0.36f, 0.38f, 0.33f))
        )
        return Result.Success(trends)
    }

    override suspend fun getEmotionEvents(emotion: String): Result<List<String>> {
        val events = when (emotion) {
            "행복" -> listOf(
                "좋은 소식을 들은 날",
                "친구와의 즐거운 만남",
                "목표를 달성한 기쁨",
                "자연 속에서의 휴식",
                "칭찬을 받아 기분 좋은 하루"
            )
            "슬픔" -> listOf(
                "이별의 순간을 되새긴 날",
                "실패로 좌절한 경험",
                "외로움을 느낀 밤",
                "눈물로 하루를 마무리",
                "낙심했던 하루의 기록"
            )
            "불안" -> listOf(
                "시험을 앞둔 긴장된 밤",
                "중요한 발표를 준비하며",
                "낯선 환경에서의 불안감",
                "건강에 대한 걱정이 많았던 날",
                "불확실한 미래를 떠올린 하루"
            )
            "무기력" -> listOf(
                "아침에 일어나기 힘든 날",
                "집중력이 떨어진 하루",
                "의욕이 사라진 저녁",
                "피로가 누적된 주말",
                "하루 종일 누워 있었던 날"
            )
            "흥미" -> listOf(
                "새로운 취미를 발견한 날",
                "좋은 책을 읽으며 몰입한 시간",
                "새로운 아이디어가 떠오른 순간",
                "재미있는 대화를 나눈 하루",
                "도전적인 과제에 흥미를 느낀 시간"
            )
            else -> listOf(
                "평범하지만 의미 있었던 하루",
                "작은 변화가 느껴진 날",
                "새로운 생각을 정리한 저녁",
                "스스로를 돌아본 시간",
                "조용하고 평화로운 하루"
            )
        }
        return Result.Success(events)
    }

    override suspend fun getJournalKeywords(): Result<List<String>> {
        val keywords = listOf(
            "행복", "불안", "슬픔", "기대", "설렘", "외로움", "만족", "후회", "공허함",
            "출근", "공부", "친구", "가족", "여행", "날씨", "커피", "점심", "산책", "대화",
            "자기이해", "회복", "성장", "감사", "치유", "도전", "목표", "몰입", "휴식", "성취",
            "자유", "평화", "열정", "포용", "균형", "안정", "사랑", "용기", "변화", "집중",
            "배움", "공감", "자존감", "용서", "신뢰", "소망", "기억", "회상", "실패", "성공",
            "극복", "인내", "위로", "격려", "성찰", "창의성", "여유", "열망", "고민", "성취감",
            "목표의식", "자신감", "반성", "깨달음", "희망", "도전정신", "감정", "생각", "행동",
            "습관", "일상", "감동", "기분", "마음", "스트레스", "편안함", "불편함", "변화무쌍",
            "소통", "결심", "도움", "성실", "책임", "성장통", "용서함", "희생", "행동력", "도약",
            "도전과제", "목표달성", "감정표현", "자기개발", "자기관리", "내면", "외부환경", "성취욕",
            "자기존중", "감정기복", "마음챙김", "자기반성", "인생", "삶", "꿈", "미래", "과거", "현재"
        )
        return Result.Success(keywords)
    }
}