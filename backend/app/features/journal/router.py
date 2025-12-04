from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query, status
from fastapi.security import HTTPBearer

from app.common.authorization import get_current_user
from app.features.journal.errors import JournalBadRequestError
from app.features.journal.schemas.requests import (
    ImageCompletionRequest,
    ImageGenerateRequest,
    ImageUploadRequest,
    JournalCreateRequest,
    JournalUpdateRequest,
)
from app.features.journal.schemas.responses import (
    ImageGenerateResponse,
    JournalCursorResponse,
    JournalImageResponse,
    JournalKeywordsListResponse,
    JournalResponse,
    PresignedUrlResponse,
)
from app.features.journal.service import JournalOpenAIService, JournalService
from app.features.user.models import User

router = APIRouter(prefix="/journal", tags=["journal"])
security = HTTPBearer()


@router.post(
    "/",
    response_model=JournalResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new journal entry",
)
def create_journal_entry(
    journal: JournalCreateRequest,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> JournalResponse:
    created_journal = journal_service.create_journal(
        user_id=user.id,
        title=journal.title,
        content=journal.content,
        emotions=journal.emotions,
        gratitude=journal.gratitude,
    )
    return JournalResponse.from_journal(created_journal)


@router.get(
    "/me",
    response_model=JournalCursorResponse,
    status_code=status.HTTP_200_OK,
    summary="Get all journal entries of logged in user with pagination",
)
def get_journal_entries_by_user(
    journal_service: Annotated[JournalService, Depends()],
    limit: int = Query(default=10, le=50),
    cursor: int | None = Query(
        None, description="ID of the last journal for cursor pagination"
    ),
    user: User = Depends(get_current_user),
) -> JournalCursorResponse:
    journals = journal_service.list_journals_by_user(user.id, limit, cursor)
    return JournalCursorResponse.from_journals(journals, limit)


@router.get(
    "/search",
    response_model=JournalCursorResponse,
    status_code=status.HTTP_200_OK,
    summary="Search journals (title/date range)",
    description="Search journals by optional title and date range; provide both start_date and end_date when filtering by date.",
)
def search_journals(
    journal_service: Annotated[JournalService, Depends()],
    start_date: date | None = Query(None, description="Start date (YYYY-MM-DD)"),
    end_date: date | None = Query(None, description="End date (YYYY-MM-DD)"),
    title: str | None = Query(None, description="Journal title (partial match)"),
    limit: int = Query(default=10, le=50),
    cursor: int | None = Query(
        None, description="ID of the last journal for cursor pagination"
    ),
    user: User = Depends(get_current_user),
) -> JournalCursorResponse:
    if not any([title, start_date, end_date]):
        raise JournalBadRequestError("At least one query parameter must be provided.")
    if (start_date and not end_date) or (end_date and not start_date):
        raise JournalBadRequestError("Both start_date and end_date must be provided.")
    journals = journal_service.search_journals(
        user_id=user.id,
        start_date=start_date,
        end_date=end_date,
        title=title,
        limit=limit,
        cursor=cursor,
    )
    return JournalCursorResponse.from_journals(journals, limit)


@router.get(
    "/search-keyword",
    response_model=JournalCursorResponse,
    status_code=status.HTTP_200_OK,
    summary="Search journals by keyword",
    description="Return journals containing the specified keyword.",
)
def search_journals_by_keyword(
    journal_service: Annotated[JournalService, Depends()],
    keyword: str = Query(..., description="Keyword to search for"),
    limit: int = Query(default=10, le=50),
    cursor: int | None = Query(
        None, description="ID of the last journal for cursor pagination"
    ),
    user: User = Depends(get_current_user),
) -> JournalCursorResponse:
    if not keyword.strip():
        raise JournalBadRequestError("keyword must not be blank")
    journals = journal_service.get_journals_by_keyword(user.id, keyword, limit, cursor)
    return JournalCursorResponse.from_journals(journals, limit)


@router.get(
    "/{journal_id}",
    response_model=JournalResponse,
    status_code=status.HTTP_200_OK,
    summary="Get a journal entry by ID",
)
def get_journal_entry(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> JournalResponse:
    journal = journal_service.get_owned_journal(journal_id, user.id)
    return JournalResponse.from_journal(journal)


@router.patch(
    "/{journal_id}",
    status_code=status.HTTP_200_OK,
    summary="Update a journal entry by ID",
)
def update_journal_entry(
    journal_id: int,
    journal: JournalUpdateRequest,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> str:
    journal_service.get_owned_journal(journal_id, user.id)
    journal_service.update_journal(
        journal_id=journal_id,
        title=journal.title,
        content=journal.content,
        gratitude=journal.gratitude,
    )
    return "Update Success"


@router.delete(
    "/{journal_id}",
    status_code=status.HTTP_200_OK,
    summary="Delete a journal entry by ID",
)
def delete_journal_entry(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> str:
    journal_service.get_owned_journal(journal_id, user.id)
    journal_service.delete_journal(journal_id)
    return "Deletion Success"


@router.post(
    "/{journal_id}/image",
    response_model=PresignedUrlResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Generate a presigned URL for image upload",
    description="This is async endpoint because it interacts with external services (AWS S3).",
)
async def generate_image_upload_url(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    payload: ImageUploadRequest,
    user: User = Depends(get_current_user),
) -> PresignedUrlResponse:
    journal_service.get_owned_journal(journal_id, user.id)
    return await journal_service.create_image_presigned_url(
        journal_id=journal_id, payload=payload
    )


@router.post(
    "/{journal_id}/image/complete",
    response_model=JournalImageResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Complete image upload",
    description="This endpoint is called to complete the image upload process; After client uploads the image to the presigned URL, it calls this endpoint to save the JournalImage record in DB.",
)
async def complete_image_upload(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    payload: ImageCompletionRequest,
    user: User = Depends(get_current_user),
) -> JournalImageResponse:
    journal_service.get_owned_journal(journal_id, user.id)
    journal_image = await journal_service.complete_image_upload(
        journal_id=journal_id, payload=payload
    )
    return JournalImageResponse.from_journal_image(journal_image)


@router.post(
    "/image/generate",
    response_model=ImageGenerateResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Request AI image generation for a journal entry",
    description="Requests AI image generation to DALL E based on provided journal content and return to client.",
)
async def request_journal_image_generation(
    journal_openai_service: Annotated[JournalOpenAIService, Depends()],
    request: ImageGenerateRequest,
    user: User = Depends(get_current_user),
) -> ImageGenerateResponse:
    image_base64 = await journal_openai_service.request_image_generation(
        request=request, user=user
    )
    return ImageGenerateResponse(image_base64=image_base64)


@router.post(
    "/{journal_id}/analyze",
    status_code=status.HTTP_201_CREATED,
    response_model=JournalKeywordsListResponse,
    summary="Analyze journal and store keywords with emotion associations",
    description="Using OpenAI's LLM, extract keywords from the journal and associate them with emotions present in the journal.",
)
async def analyze_journal(
    journal_id: int,
    journal_openai_service: Annotated[JournalOpenAIService, Depends()],
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> JournalKeywordsListResponse:
    journal_service.get_owned_journal(journal_id, user.id)
    created_keywords_list = (
        await journal_openai_service.extract_keywords_with_emotion_associations(
            journal_id=journal_id
        )
    )
    return JournalKeywordsListResponse.from_journal_keywords(created_keywords_list)
