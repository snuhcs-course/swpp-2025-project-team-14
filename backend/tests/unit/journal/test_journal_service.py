import os
from unittest.mock import AsyncMock, MagicMock, Mock

import pytest

from app.features.journal.errors import ImageUploadError, JournalNotFoundError
from app.features.journal.facade import JournalImageFacade
from app.features.journal.models import Journal, JournalImage
from app.features.journal.repository import JournalRepository, S3Repository
from app.features.journal.schemas.requests import (
    ImageCompletionRequest,
    ImageGenerateRequest,
    ImageUploadRequest,
)
from app.features.journal.service import JournalOpenAIService, JournalService
from app.features.journal.strategies import ImageStyleFactory, ImageStyleStrategy
from app.features.user.models import User

# --- 픽스처: Mock 객체 준비 ---


@pytest.fixture
def mock_journal_repo() -> Mock:
    return Mock(spec=JournalRepository)


@pytest.fixture
def mock_s3_repo() -> Mock:
    repo = Mock(spec=S3Repository)
    repo.generate_upload_url = AsyncMock()
    repo.check_file_exists = AsyncMock()
    repo.delete_object = AsyncMock()
    return repo


@pytest.fixture
def mock_image_facade() -> Mock:
    """JournalImageFacade의 가짜 Mock 객체 (Service 테스트용)"""
    facade = Mock(spec=JournalImageFacade)
    facade.initiate_image_upload = AsyncMock()
    facade.finalize_image_upload = AsyncMock()
    return facade


@pytest.fixture
def mock_style_factory() -> Mock:
    """ImageStyleFactory의 가짜 Mock 객체 (OpenAI Service 테스트용)"""
    return Mock(spec=ImageStyleFactory)


@pytest.fixture
def journal_service(mock_journal_repo: Mock, mock_image_facade: Mock) -> JournalService:
    """
    JournalService에는 이제 S3Repo가 아닌 Facade가 주입됩니다.
    """
    return JournalService(
        journal_repository=mock_journal_repo,
        image_facade=mock_image_facade,
    )


@pytest.fixture
def journal_image_facade(
    mock_journal_repo: Mock, mock_s3_repo: Mock
) -> JournalImageFacade:
    """
    실제 로직 테스트를 위해 Facade 인스턴스를 생성합니다. (Facade 테스트용)
    """
    return JournalImageFacade(
        journal_repository=mock_journal_repo, s3_repository=mock_s3_repo
    )


# --- JournalService 테스트 케이스 (CRUD) ---


def test_create_journal_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    user_id = 1
    title = "테스트 일기"
    content = "내용"
    emotions = {"happy": 5}
    gratitude = "감사"
    mock_journal = Journal(id=100, user_id=user_id, title=title)
    mock_journal_repo.add_journal.return_value = mock_journal

    result = journal_service.create_journal(
        user_id, title, content, emotions, gratitude
    )

    mock_journal_repo.add_journal.assert_called_once()
    assert result == mock_journal


def test_get_journal_success(journal_service: JournalService, mock_journal_repo: Mock):
    journal_id = 1
    mock_journal = Journal(id=journal_id)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    result = journal_service.get_journal(journal_id)
    assert result == mock_journal


def test_get_journal_not_found(
    journal_service: JournalService, mock_journal_repo: Mock
):
    mock_journal_repo.get_journal_by_id.return_value = None
    with pytest.raises(JournalNotFoundError):
        journal_service.get_journal(999)


def test_delete_journal_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    journal_id = 1
    mock_journal = Journal(id=journal_id)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    journal_service.delete_journal(journal_id)
    mock_journal_repo.delete_journal.assert_called_once_with(mock_journal)


def test_update_journal_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    journal_id = 1
    mock_journal = Journal(id=journal_id)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    journal_service.update_journal(journal_id, title="New")
    mock_journal_repo.update_journal.assert_called_once()


def test_search_journals_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    mock_journal_repo.search_journals.return_value = []
    journal_service.search_journals(1, title="Test")
    mock_journal_repo.search_journals.assert_called_once()


# --- JournalService 이미지 테스트 (Facade 위임 확인) ---


@pytest.mark.asyncio
async def test_service_delegates_create_presigned_url(
    journal_service: JournalService, mock_image_facade: Mock
):
    """
    [Service] create_image_presigned_url이 Facade를 호출하는지 테스트
    """
    journal_id = 1
    payload = ImageUploadRequest(filename="test.jpg", content_type="image/jpeg")
    mock_response = MagicMock()
    mock_image_facade.initiate_image_upload.return_value = mock_response

    result = await journal_service.create_image_presigned_url(journal_id, payload)

    mock_image_facade.initiate_image_upload.assert_awaited_once_with(
        journal_id, payload.filename, payload.content_type
    )
    assert result == mock_response


@pytest.mark.asyncio
async def test_service_delegates_complete_upload(
    journal_service: JournalService, mock_image_facade: Mock
):
    """
    [Service] complete_image_upload가 Facade를 호출하는지 테스트
    """
    journal_id = 1
    payload = ImageCompletionRequest(s3_key="key")
    mock_image = JournalImage(id=1)
    mock_image_facade.finalize_image_upload.return_value = mock_image

    result = await journal_service.complete_image_upload(journal_id, payload)

    mock_image_facade.finalize_image_upload.assert_awaited_once_with(
        journal_id, payload.s3_key
    )
    assert result == mock_image


# --- JournalImageFacade 테스트 (기존 Service 로직 이동) ---


@pytest.mark.asyncio
async def test_facade_initiate_upload_success(
    journal_image_facade: JournalImageFacade,
    mock_journal_repo: Mock,
    mock_s3_repo: Mock,
):
    """
    [Facade] initiate_image_upload 성공 테스트 (UUID 생성, S3 URL 발급)
    """
    journal_id = 1
    filename = "test.jpg"
    content_type = "image/jpeg"

    mock_journal_repo.get_journal_by_id.return_value = Journal(id=journal_id)
    mock_s3_repo.generate_upload_url.return_value = {
        "presigned_url": "http://pre",
        "file_url": "http://file",
    }

    result = await journal_image_facade.initiate_image_upload(
        journal_id, filename, content_type
    )

    mock_s3_repo.generate_upload_url.assert_awaited_once()
    assert result.presigned_url == "http://pre"
    assert "images/journals/1/" in result.s3_key


@pytest.mark.asyncio
async def test_facade_initiate_upload_not_found(
    journal_image_facade: JournalImageFacade, mock_journal_repo: Mock
):
    mock_journal_repo.get_journal_by_id.return_value = None
    with pytest.raises(JournalNotFoundError):
        await journal_image_facade.initiate_image_upload(999, "t.jpg", "image/jpeg")


@pytest.mark.asyncio
async def test_facade_finalize_upload_success(
    journal_image_facade: JournalImageFacade,
    mock_journal_repo: Mock,
    mock_s3_repo: Mock,
):
    """
    [Facade] finalize_image_upload 성공 테스트 (S3 확인, DB 갱신)
    """
    journal_id = 1
    s3_key = "some/key.jpg"

    mock_s3_repo.check_file_exists.return_value = True
    mock_journal_repo.get_journal_by_id.return_value = Journal(id=journal_id)
    mock_journal_repo.get_image_by_journal_id.return_value = None

    expected_image = JournalImage(id=10, s3_key=s3_key)
    mock_journal_repo.replace_journal_image.return_value = expected_image

    result = await journal_image_facade.finalize_image_upload(journal_id, s3_key)

    mock_s3_repo.check_file_exists.assert_awaited_once_with(s3_key)
    mock_journal_repo.replace_journal_image.assert_called_once()
    assert result == expected_image


@pytest.mark.asyncio
async def test_facade_finalize_upload_missing_file(
    journal_image_facade: JournalImageFacade, mock_s3_repo: Mock
):
    mock_s3_repo.check_file_exists.return_value = False
    with pytest.raises(ImageUploadError):
        await journal_image_facade.finalize_image_upload(1, "missing.jpg")


# --- JournalOpenAIService 테스트 (Strategy/Factory 적용) ---


@pytest.mark.asyncio
async def test_request_image_generation_with_strategy(
    test_user: User, mocker, mock_style_factory: Mock
):
    """
    [Service] request_image_generation 테스트 (Factory 및 Strategy 패턴 검증)
    """
    # 1. Setup Mocks
    mocker.patch.dict(os.environ, {"OPENAI_API_KEY": "fake"})

    # LangChain/OpenAI Mock
    mock_chain = AsyncMock()
    mock_structured_llm = MagicMock()
    mock_structured_llm.with_structured_output.return_value = mock_chain
    mocker.patch(
        "app.features.journal.service.ChatOpenAI", return_value=mock_structured_llm
    )
    mocker.patch("app.features.journal.service.AsyncOpenAI", return_value=AsyncMock())
    mocker.patch(
        "app.features.journal.service._load_prompt", return_value="keyword_prompt"
    )

    # [Strategy & Factory Mocking]
    mock_strategy = Mock(spec=ImageStyleStrategy)
    mock_strategy.get_system_prompt.return_value = "Strategy Prompt"

    mock_style_factory.create_strategy.return_value = mock_strategy

    # Service 인스턴스 생성 (Factory 주입)
    service = JournalOpenAIService(
        journal_repository=MagicMock(), style_factory=mock_style_factory
    )

    # 내부 헬퍼 메서드 Mocking (실제 LLM 호출 방지)
    mocker.patch.object(
        service,
        "_generate_scene_prompt_from_diary",
        new_callable=AsyncMock,
        return_value="Scene",
    )
    mocker.patch.object(
        service,
        "_generate_image_from_prompt",
        new_callable=AsyncMock,
        return_value="B64Image",
    )

    # 2. Execution
    request = ImageGenerateRequest(content="Diary", style="natural")
    result = await service.request_image_generation(request, test_user)

    # 3. Verification
    # 3-1. Factory가 올바른 스타일로 호출되었는가?
    mock_style_factory.create_strategy.assert_called_once_with("natural")

    # 3-2. Strategy에서 프롬프트를 가져왔는가?
    mock_strategy.get_system_prompt.assert_called_once()

    # 3-3. 가져온 프롬프트("Strategy Prompt")가 LLM 호출에 사용되었는가?
    service._generate_scene_prompt_from_diary.assert_awaited_once()
    call_args = service._generate_scene_prompt_from_diary.call_args
    assert "Strategy Prompt" in call_args[0]  # 인자 중 프롬프트가 포함되어야 함

    assert result == "B64Image"
