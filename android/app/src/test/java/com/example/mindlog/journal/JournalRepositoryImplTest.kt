package com.example.mindlog.features.journal.data.repository

import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException

class JournalRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var journalApi: JournalApi
    private lateinit var journalRepository: JournalRepositoryImpl

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .setLenient()
            .create()

        journalApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(JournalApi::class.java)

        journalRepository = JournalRepositoryImpl(journalApi)
    }


    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    // --- getJournals Tests ---
    @Test
    fun `getJournals - calls correct api endpoint and returns data`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { "items": [{"id": 1, "title": "Test"}], "next_cursor": 2 }
                """.trimIndent()
            )
        )

        // When
        val result = journalRepository.getJournals(limit = 10, cursor = null)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/journal/me?"))
        assertTrue(request.path!!.contains("limit=10"))
        assertEquals(1, result.items.size)
        assertEquals(2, result.nextCursor)
    }

    // --- getJournalById Tests ---
    @Test
    fun `getJournalById - success - calls correct endpoint and returns item`() = runTest {
        // Given
        val journalId = 123
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { "id": $journalId, "title": "Test Journal", "content": "Content", "emotions": [], "gratitude": "", "image_s3_keys": null, "created_at": "2025-01-01T12:00:00", "keywords": [] }
                """.trimIndent()
            )
        )

        // When
        val result = journalRepository.getJournalById(journalId)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("/journal/$journalId", request.path)
        assertEquals(journalId, result.id)
        assertEquals("Test Journal", result.title)
    }

    @Test(expected = HttpException::class)
    fun `getJournalById - failure - throws HttpException for 404`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse().setResponseCode(404).setBody("""{ "detail": "Not Found" }""")
        )
        // When
        journalRepository.getJournalById(999)
        // Then: HttpException is thrown
    }

    // --- createJournal Tests ---
    @Test
    fun `createJournal - sends correct body and returns response`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                { "id": 1, "title": "New Journal", "content": "New Content", "emotions": [], "created_at": "2025-01-01T12:00:00" }
                """.trimIndent()
            )
        )

        // When
        val response = journalRepository.createJournal(
            "New Journal",
            "New Content",
            mapOf("happy" to 4),
            "Gratitude"
        )

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/journal/", request.path)
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains(""""title":"New Journal""""))
        assertTrue(requestBody.contains(""""happy":4"""))
        assertEquals(1, response.id)
    }

    // --- updateJournal Tests ---
    @Test
    fun `updateJournal - calls correct endpoint with patch body`() = runTest {
        // Given
        val journalId = 77
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody("\"Update Success\"")
        )
        val updateRequest = UpdateJournalRequest(
            title = "Updated Title",
            content = null,
            gratitude = "Updated Gratitude"
        )

        // When
        journalRepository.updateJournal(journalId, updateRequest)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/journal/$journalId", request.path)
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains(""""title":"Updated Title""""))
        assertTrue(!requestBody.contains(""""content"""")) // null fields should not be in the body
    }

    // --- deleteJournal Tests ---
    @Test
    fun `deleteJournal - calls correct endpoint`() = runTest {
        // Given
        val journalId = 55
        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        // When
        journalRepository.deleteJournal(journalId)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/journal/$journalId", request.path)
    }

    // --- searchJournals Tests ---
    @Test
    fun `searchJournals - calls correct api endpoint with title`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { "items": [], "next_cursor": null }
                """.trimIndent()
            )
        )

        // When
        journalRepository.searchJournals(null, null, "검색어", 10, null)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/journal/search?"))
        assertTrue(request.path!!.contains("title=%EA%B2%80%EC%83%89%EC%96%B4")) // URL-encoded "검색어"
        assertTrue(request.path!!.contains("limit=10"))
    }

    // --- uploadJournalImage Tests ---
    @Test
    fun `uploadJournalImage - executes 3 steps successfully`() = runTest {
        // Given
        val journalId = 101
        val s3Key = "uploads/image.jpg"
        val presignedUrl = mockWebServer.url("/s3-fake-upload")

        // 1. Presigned URL 응답
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "presigned_url": "$presignedUrl", "s3_key": "$s3Key" }"""
            )
        )
        // 2. S3 업로드 응답
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        // 3. 업로드 완료 보고 응답
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "id": 1, "journal_id": $journalId, "s3_key": "$s3Key", "created_at": "2025-01-01T12:00:00" }"""
            )
        )

        // When
        journalRepository.uploadJournalImage(
            journalId,
            "test-data".toByteArray(),
            "image/jpeg",
            "image.jpg"
        )

        // Then
        val req1 = mockWebServer.takeRequest() // Presigned URL
        assertEquals("POST", req1.method)
        assertEquals("/journal/$journalId/image", req1.path)

        val req2 = mockWebServer.takeRequest() // S3 Upload
        assertEquals("PUT", req2.method)
        assertEquals("/s3-fake-upload", req2.path)

        val req3 = mockWebServer.takeRequest() // Complete Upload
        assertEquals("POST", req3.method)
        assertEquals("/journal/$journalId/image/complete", req3.path)
    }

    @Test(expected = RuntimeException::class)
    fun `uploadJournalImage - throws exception on S3 upload failure`() = runTest {
        // Given
        val journalId = 101
        val presignedUrl = mockWebServer.url("/s3-fake-upload")

        // 1. Presigned URL 응답 (성공)
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "presigned_url": "$presignedUrl", "s3_key": "key" }"""
            )
        )
        // 2. S3 업로드 응답 (실패)
        mockWebServer.enqueue(
            MockResponse().setResponseCode(403).setBody("Forbidden")
        )

        // When
        journalRepository.uploadJournalImage(
            journalId,
            "data".toByteArray(),
            "image/jpeg",
            "file.jpg"
        )
        // Then: RuntimeException is thrown
    }

    // --- generateImage Tests ---
    @Test
    fun `generateImage - success - returns base64 string`() = runTest {
        // Given
        val base64String =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "image_base64": "$base64String" }"""
            )
        )

        // When
        val result = journalRepository.generateImage("style", "content")

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/journal/image/generate", request.path)
        assertEquals(base64String, result)
    }

    @Test(expected = RuntimeException::class)
    fun `generateImage - failure - throws correct exception on timeout`() = runTest {
        // Given: suspend 함수에 대해 SocketTimeoutException 던지도록 설정
        val mockApi: JournalApi = mock()
        doThrow(SocketTimeoutException())
            .whenever(mockApi)
            .generateImage(any())

        val repoWithMockApi = JournalRepositoryImpl(mockApi)

        // When
        repoWithMockApi.generateImage("style", "content")
        // Then: RuntimeException is thrown (Repository에서 wrap했다고 가정)
    }

    // --- extractKeywords Tests ---
    @Test
    fun `extractKeywords - success - calls correct endpoint and returns keywords`() = runTest {
        // Given
        val journalId = 200
        // ✨ [핵심 수정 1] Mock 응답의 JSON 구조를 실제 DTO와 동일하게 "data" 필드를 사용하도록 변경
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { 
                    "data": [{"keyword": "test", "emotion": "happy", "summary": "요약", "weight": 0.9}] 
                }
                """.trimIndent()
            )
        )

        // When
        val result = journalRepository.extractKeywords(journalId)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/journal/$journalId/analyze", request.path)

        assertNotNull(result.data)
        assertEquals(1, result.data.size)
        assertEquals("test", result.data.first().keyword)
    }

    @Test(expected = RuntimeException::class)
    fun `uploadJournalImage - throws exception on presigned URL failure`() = runTest {
        // Given: 1단계 Presigned URL 요청이 500 서버 에러로 실패하도록 설정
        val journalId = 101
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500).setBody("Internal Server Error")
        )

        // When: 이미지 업로드를 시도
        journalRepository.uploadJournalImage(
            journalId = journalId,
            imageBytes = "test-data".toByteArray(),
            contentType = "image/jpeg",
            fileName = "image.jpg"
        )

    }
    @Test(expected = RuntimeException::class)
    fun `uploadJournalImage - throws exception on complete upload failure`() = runTest {
        // Given: 1, 2단계는 성공하고 3단계에서 실패하도록 응답 설정
        val journalId = 101
        val s3Key = "uploads/image.jpg"
        val presignedUrl = mockWebServer.url("/s3-fake-upload")

        // 1. Presigned URL 응답 (성공)
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "presigned_url": "$presignedUrl", "s3_key": "$s3Key" }"""
            )
        )
        // 2. S3 업로드 응답 (성공)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // 3. 업로드 완료 보고 응답 (실패)
        mockWebServer.enqueue(
            MockResponse().setResponseCode(503).setBody("Service Unavailable")
        )

        // When: 이미지 업로드를 시도
        journalRepository.uploadJournalImage(
            journalId = journalId,
            imageBytes = "test-data".toByteArray(),
            contentType = "image/jpeg",
            fileName = "image.jpg"
        )
    }
}
