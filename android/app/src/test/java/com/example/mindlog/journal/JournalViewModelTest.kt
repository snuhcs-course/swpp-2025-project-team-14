package com.example.mindlog.journal

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.BuildConfig
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.SearchJournalsUseCase
import com.example.mindlog.features.journal.presentation.list.JournalViewModel
import com.example.mindlog.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// ... (이하 클래스 코드는 그대로)


@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var getJournalsUseCase: GetJournalUseCase
    private lateinit var searchJournalsUseCase: SearchJournalsUseCase
    private lateinit var getJournalByIdUseCase: GetJournalByIdUseCase

    // ✨ ViewModel을 더 이상 spy로 만들 필요가 없습니다.
    private lateinit var viewModel: JournalViewModel

    @Before
    fun setup() {
        // ✨ 모든 UseCase는 단순한 mock으로 유지합니다.
        getJournalsUseCase = mock()
        searchJournalsUseCase = mock()
        getJournalByIdUseCase = mock()

        viewModel = JournalViewModel(
            getJournalsUseCase,
            searchJournalsUseCase,
            getJournalByIdUseCase
        )
    }

    @Test
    fun `loadJournals success - updates journals LiveData and sets loading to false`() = runTest {
        // Given
        val dummyResponse = JournalListResponse(
            items = listOf(createDummyJournalItem(1), createDummyJournalItem(2)),
            nextCursor = 3
        )
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(dummyResponse)

        // When: loadJournals()만 호출
        viewModel.loadJournals()

        // Then
        advanceUntilIdle() // 코루틴 작업 완료 대기

        // UseCase가 정확히 1번 호출되었는지 검증
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)

        // 최종 상태 검증
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
        assertEquals(2, viewModel.journals.value?.size)
        assertEquals(1, viewModel.journals.value?.first()?.id)
        assertEquals(false, viewModel.isLastPage)
    }

    @Test
    fun `loadJournals failure - sets error message and sets loading to false`() = runTest {
        // Given
        val errorMessage = "네트워크 오류 발생"
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.loadJournals()

        // Then
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertTrue(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
        assertTrue(viewModel.journals.value?.isEmpty() ?: true)
    }

    @Test
    fun `loadMoreJournals - appends new items to existing list`() = runTest {
        // Given: 첫 페이지 로드
        val firstResponse = JournalListResponse(items = listOf(createDummyJournalItem(1)), nextCursor = 2)
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = null)).thenReturn(firstResponse)
        viewModel.loadJournals()
        advanceUntilIdle()
        assertEquals(1, viewModel.journals.value?.size)

        // Given: 두 번째 페이지 응답 설정
        val secondResponse = JournalListResponse(items = listOf(createDummyJournalItem(2)), nextCursor = null)
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = 2)).thenReturn(secondResponse)

        // When: 추가 로드
        viewModel.loadMoreJournals()
        advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.journals.value?.size)
        assertEquals(2, viewModel.journals.value?.get(1)?.id)
        assertEquals(true, viewModel.isLastPage)
        // 첫 페이지(cursor=null), 두 번째 페이지(cursor=2) 각각 1번씩 호출되었는지 검증
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = 2)
    }

    @Test
    fun `loadJournals with search query - calls searchJournalsUseCase`() = runTest {
        // Given
        val query = "검색어"
        viewModel.searchQuery.value = query
        val dummyResponse = JournalListResponse(items = listOf(createDummyJournalItem(10)), nextCursor = null)
        whenever(searchJournalsUseCase.invoke(any(), any(), any(), any(), anyOrNull())).thenReturn(dummyResponse)

        // When: 검색 조건이 있는 상태에서 로드
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        verify(searchJournalsUseCase, times(1)).invoke(
            startDate = null,
            endDate = null,
            title = query,
            limit = 10,
            cursor = null
        )
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull()) // 일반 조회사용X
    }

    @Test
    fun `updateOrRemoveJournalEntry with deletedId - removes item from list`() = runTest {
        // Given
        val initialList = listOf(
            JournalEntry(1, "제목1", "내용1", java.util.Date(), null, emptyList(), emptyList()),
            JournalEntry(2, "제목2", "내용2", java.util.Date(), null, emptyList(), emptyList())
        )
        viewModel.journals.value = initialList
        assertEquals(2, viewModel.journals.value?.size)

        // When
        viewModel.updateOrRemoveJournalEntry(updatedId = null, deletedId = 2)
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.journals.value?.size)
        assertNull(viewModel.journals.value?.find { it.id == 2 })
    }

    @Test
    fun `updateOrRemoveJournalEntry with updatedId - updates item in list`() = runTest {
        // Given
        val initialList = listOf(JournalEntry(1, "원본 제목", "원본 내용", java.util.Date(), null, emptyList(), emptyList()))
        viewModel.journals.value = initialList
        val updatedItemResponse = createDummyJournalItem(1).copy(title = "수정된 제목")
        whenever(getJournalByIdUseCase.invoke(1)).thenReturn(updatedItemResponse)

        // When
        viewModel.updateOrRemoveJournalEntry(updatedId = 1, deletedId = null)
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.journals.value?.size)
        assertEquals("수정된 제목", viewModel.journals.value?.first()?.title)
        verify(getJournalByIdUseCase, times(1)).invoke(1)
    }

    @Test
    fun `setDateRange - only updates state, does not call use case`() = runTest {
        // Given
        val startDate = "2025-01-01"
        val endDate = "2025-01-31"

        // When: 날짜 범위만 설정
        viewModel.setDateRange(startDate, endDate)
        advanceUntilIdle()

        // Then: StateFlow 값만 변경되었는지 검증
        assertEquals(startDate, viewModel.startDate.value)
        assertEquals(endDate, viewModel.endDate.value)

        // Then: UseCase가 호출되지 않았는지 검증
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
    }

    @Test
    fun `clearSearchConditions and load - resets all search states and reloads`() = runTest {
        // Given
        viewModel.searchQuery.value = "초기 검색어"
        viewModel.startDate.value = "2025-01-01"
        viewModel.endDate.value = "2025-01-31"
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(JournalListResponse(emptyList(), null))

        // When
        viewModel.clearSearchConditions() // 상태만 초기화
        viewModel.loadJournals() // 수동으로 로드
        advanceUntilIdle()

        // Then: 모든 검색 관련 StateFlow 값이 null로 초기화되었는지 검증
        assertNull(viewModel.searchQuery.value)
        assertNull(viewModel.startDate.value)
        assertNull(viewModel.endDate.value)

        // Then: 일반 목록 조회(getJournalsUseCase)가 호출되었는지 검증
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
    }

    @Test
    fun `loadJournals with keywords - maps keywords correctly`() = runTest {
        // Given
        val dummyResponse = JournalListResponse(
            items = listOf(
                createDummyJournalItem(1).copy(
                    keywords = listOf(
                        com.example.mindlog.features.journal.data.dto.KeywordResponse(
                            keyword = "테스트키워드",
                            emotion = "happy",
                            summary = "요약",
                            weight = 0.9f
                        )
                    )
                )
            ),
            nextCursor = null
        )
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(dummyResponse)

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val resultingJournals = viewModel.journals.value
        assertNotNull(resultingJournals)
        assertEquals(1, resultingJournals?.size)
        val firstJournal = resultingJournals?.first()
        assertNotNull(firstJournal?.keywords)
        assertEquals(1, firstJournal?.keywords?.size)
        assertEquals("테스트키워드", firstJournal?.keywords?.first()?.keyword)
    }

    private fun createDummyJournalItem(id: Int): JournalItemResponse {
        return JournalItemResponse(
            id = id,
            title = "테스트 일기 $id",
            content = "테스트 내용 $id",
            emotions = emptyList(),
            gratitude = "감사일기 $id",
            imageS3Keys = null,
            createdAt = "2025-01-01T12:00:00",
            keywords = emptyList()
        )
    }

    @Test
    fun `startObservingSearchQuery - calls loadJournals after debounce`() = runTest {
        // Given: 검색 API가 호출될 때의 응답 설정
        whenever(searchJournalsUseCase.invoke(any(), any(), any(), any(), anyOrNull()))
            .thenReturn(JournalListResponse(emptyList(), null))

        // When: 검색어 관찰 시작
        viewModel.startObservingSearchQuery()

        // When: 검색어 변경
        viewModel.searchQuery.value = "새로운 검색어"

        // Then: debounce 시간(500ms)이 지나고, advanceUntilIdle로 모든 작업이 완료될 때까지 기다림
        advanceUntilIdle()

        // Then: loadJournals가 호출되었고, 그 결과로 searchJournalsUseCase가 1번 호출되었는지 검증
        verify(searchJournalsUseCase, times(1)).invoke(
            startDate = null,
            endDate = null,
            title = "새로운 검색어",
            limit = 10,
            cursor = null
        )
    }

    @Test
    fun `clearSearchAndReload - resets states and calls getJournalsUseCase`() = runTest {
        // Given: 검색 조건이 이미 설정된 상태
        viewModel.searchQuery.value = "기존 검색어"
        viewModel.startDate.value = "2025-01-01"
        whenever(getJournalsUseCase.invoke(any(), anyOrNull()))
            .thenReturn(JournalListResponse(emptyList(), null))

        // When: clearSearchAndReload 함수 호출
        viewModel.clearSearchAndReload()
        advanceUntilIdle()

        // Then: 모든 검색 조건이 null로 초기화되었는지 검증
        assertNull(viewModel.searchQuery.value)
        assertNull(viewModel.startDate.value)
        assertNull(viewModel.endDate.value)

        // Then: loadJournals가 호출되어, 일반 목록 조회(getJournalsUseCase)가 1번 호출되었는지 검증
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
        // 검색 UseCase는 호출되지 않아야 함
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `loadJournals - does not fetch if already loading`() = runTest {
        // Given: 로딩 상태를 강제로 true로 설정
        viewModel.isLoading.value = true

        // When: 이 상태에서 loadJournals 호출
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then: 어떤 UseCase도 호출되지 않아야 함
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
    }


    @Test
    fun `toJournalEntry - maps s3Key to correct imageUrl`() = runTest {
        // Given: imageS3Keys 필드에 값이 있는 더미 데이터
        val dummyResponse = JournalListResponse(
            items = listOf(createDummyJournalItem(1).copy(imageS3Keys = "test_image.jpg")),
            nextCursor = null
        )
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(dummyResponse)

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val journal = viewModel.journals.value?.first()
        assertNotNull(journal)
        assertEquals("${BuildConfig.S3_BUCKET_URL}/test_image.jpg", journal?.imageUrl)
    }

    @Test
    fun `toJournalEntry - handles invalid date format gracefully`() = runTest {
        // Given: 잘못된 날짜 형식의 더미 데이터
        val dummyResponse = JournalListResponse(
            items = listOf(createDummyJournalItem(1).copy(createdAt = "INVALID-DATE-FORMAT")),
            nextCursor = null
        )
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(dummyResponse)
        val timeBeforeTest = System.currentTimeMillis()

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val journal = viewModel.journals.value?.first()
        assertNotNull(journal)
        // 생성된 날짜가 테스트 시작 시간과 거의 같은지 확인 (대략적인 검증)
        assertTrue((journal!!.createdAt.time - timeBeforeTest) < 1000)
    }

    @Test
    fun `updateOrRemoveJournalEntry - does nothing for non-existent deletedId`() = runTest {
        // Given: 초기 목록 설정
        val initialList = listOf(JournalEntry(1, "제목1", "내용1", java.util.Date(), null, emptyList(), emptyList()))
        viewModel.journals.value = initialList

        // When: 존재하지 않는 ID(99)로 삭제 시도
        viewModel.updateOrRemoveJournalEntry(updatedId = null, deletedId = 99)
        advanceUntilIdle()

        // Then: 목록은 그대로여야 함
        assertEquals(1, viewModel.journals.value?.size)
    }

    @Test
    fun `updateOrRemoveJournalEntry - does nothing for non-existent updatedId`() = runTest {
        // Given: 초기 목록 설정
        val initialList = listOf(JournalEntry(1, "제목1", "내용1", java.util.Date(), null, emptyList(), emptyList()))
        viewModel.journals.value = initialList

        // When: 존재하지 않는 ID(99)로 수정 시도
        viewModel.updateOrRemoveJournalEntry(updatedId = 99, deletedId = null)
        advanceUntilIdle()

        // Then: getJournalByIdUseCase가 호출되지 않고, 목록은 그대로여야 함
        verify(getJournalByIdUseCase, never()).invoke(any())
        assertEquals(1, viewModel.journals.value?.size)
    }

    @Test
    fun `loadMoreJournals failure - sets error message`() = runTest {
        // Given: 첫 페이지는 성공적으로 로드
        val firstResponse = JournalListResponse(items = listOf(createDummyJournalItem(1)), nextCursor = 2)
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = null)).thenReturn(firstResponse)
        viewModel.loadJournals()
        advanceUntilIdle()

        // Given: 두 번째 페이지 로드 시 네트워크 에러 발생 설정
        val errorMessage = "더보기 로딩 실패"
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = 2)).thenThrow(RuntimeException(errorMessage))

        // When: 더보기 로드 시도
        viewModel.loadMoreJournals()
        advanceUntilIdle()

        // Then: errorMessage에 값이 설정되고, 로딩 상태가 false인지 확인
        assertEquals(false, viewModel.isLoading.value)
        assertTrue(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
    }

    @Test
    fun `updateOrRemoveJournalEntry with updatedId - reloads all on failure`() = runTest {
        // Given: 초기 목록 설정
        val initialList = listOf(JournalEntry(1, "원본 제목", "원본 내용", java.util.Date(), null, emptyList(), emptyList()))
        viewModel.journals.value = initialList

        // Given: 아이템 상세 정보 요청 시 에러 발생 설정
        whenever(getJournalByIdUseCase.invoke(1)).thenThrow(RuntimeException("상세 정보 로드 실패"))
        // Given: 전체 목록 새로고침 시의 응답 설정
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(JournalListResponse(emptyList(), null))

        // When: ID가 1인 아이템 수정 요청
        viewModel.updateOrRemoveJournalEntry(updatedId = 1, deletedId = null)
        advanceUntilIdle()

        // Then: getJournalByIdUseCase 호출 후 실패하여, 전체 목록을 다시 불러오는 getJournalsUseCase가 호출되었는지 검증
        verify(getJournalByIdUseCase, times(1)).invoke(1)
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
    }

    @Test
    fun `clearSearchConditions - only resets states`() = runTest {
        // Given: 검색 조건 설정
        viewModel.searchQuery.value = "검색어"
        viewModel.startDate.value = "2025-01-01"

        // When: 누락되었던 함수 호출 추가
        viewModel.clearSearchConditions()
        advanceUntilIdle()

        // Then: 상태만 초기화되고, 어떤 UseCase도 호출되지 않아야 함
        assertNull(viewModel.searchQuery.value)
        assertNull(viewModel.startDate.value)
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
    }
}
