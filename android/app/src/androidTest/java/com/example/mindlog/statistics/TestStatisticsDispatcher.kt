package com.example.mindlog.statistics

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest


class TestStatisticsDispatcher : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path ?: ""

        return when {
            path.startsWith("/journal/search") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "items": [
                            {
                              "id": 1,
                              "title": "가족과의 주말 여행",
                              "content": "지난 주말 가족과 함께 간 여행에서 평화롭고 즐거운 시간을 보냈다.",
                              "emotions": [
                                { "emotion": "happy", "intensity": 80 },
                                { "emotion": "calm", "intensity": 60 }
                              ],
                              "gratitude": "가족이 함께해줘서 감사했다.",
                              "image_s3_keys": "travel_image_001.jpg",
                              "created_at": "2025-11-05T14:20:00Z",
                              "keywords": [
                                { "keyword": "가족", "emotion": "happy", "summary": "가족과 함께한 즐거운 시간", "weight": 0.8 },
                                { "keyword": "여행", "emotion": "calm", "summary": "휴식과 재충전의 시간", "weight": 0.6 }
                              ]
                            },
                            {
                              "id": 2,
                              "title": "팀 프로젝트 마감",
                              "content": "프로젝트 마감 전날까지 긴장됐지만 결국 잘 끝났다.",
                              "emotions": [
                                { "emotion": "anxious", "intensity": 70 },
                                { "emotion": "satisfied", "intensity": 90 }
                              ],
                              "gratitude": "팀원들이 협력해줘서 고마웠다.",
                              "image_s3_keys": null,
                              "created_at": "2025-11-04T09:10:00Z",
                              "keywords": [
                                { "keyword": "성취", "emotion": "satisfied", "summary": "마감을 성공적으로 마친 성취감", "weight": 0.7 },
                                { "keyword": "팀워크", "emotion": "happy", "summary": "협력으로 얻은 긍정적 경험", "weight": 0.5 }
                              ]
                            }
                          ],
                          "next_cursor": null
                        }
                        """.trimIndent()
                    )
            }

            else -> MockResponse().setResponseCode(404)
        }
    }
}