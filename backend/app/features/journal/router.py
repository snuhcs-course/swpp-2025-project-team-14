from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from fastapi.security import HTTPBearer
from sqlalchemy.orm import Session

from app.common.authorization import get_current_user
from app.common.errors import PermissionDeniedError
from app.database.session import get_db_session
from app.features.journal.errors import JournalBadRequestError
from app.features.journal.schemas.requests import (
    ImageUploadRequest,
    JournalCreateRequest,
    JournalUpdateRequest,
)
from app.features.journal.schemas.responses import (
    JournalCursorEnvelope,
    JournalCursorResponse,
    JournalImageResponse,
    JournalListResponse,
    JournalListResponseEnvelope,
    JournalResponse,
    JournalResponseEnvelope,
    PresignedUrlResponse,
)
from app.features.journal.service import JournalService
from app.features.user.models import User

router = APIRouter(prefix="/journal", tags=["journal"])
security = HTTPBearer()


@router.post(
    "/",
    response_model=JournalResponseEnvelope,
    status_code=201,
    summary="Create a new journal entry",
)
def create_journal_entry(
    journal: JournalCreateRequest,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> JournalResponseEnvelope:
    created_journal = journal_service.create_journal(
        user_id=user.id,
        title=journal.title,
        content=journal.content,
        image_urls=journal.image_urls,
    )
    return JournalResponseEnvelope(data=JournalResponse.from_journal(created_journal))


@router.get(
    "/{journal_id}",
    response_model=JournalResponseEnvelope,
    status_code=200,
    summary="Get a journal entry by ID",
)
def get_journal_entry(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> JournalResponseEnvelope:
    if journal_service.get_journal_owner(journal_id) != user.id:
        raise PermissionDeniedError()
    journal = journal_service.get_journal(journal_id)
    if journal is None:
        return JournalResponseEnvelope(data=None)
    return JournalResponseEnvelope(data=JournalResponse.from_journal(journal))


@router.get(
    "/user/{user_id}",
    response_model=JournalCursorEnvelope,
    status_code=200,
    summary="Get all journal entries by user ID with pagination",
)
def get_journal_entries_by_user(
    user_id: int,
    journal_service: Annotated[JournalService, Depends()],
    limit: int = Query(default=10, le=50),
    cursor: int | None = Query(None, description="마지막으로 본 Journal의 ID"),
    user: User = Depends(get_current_user),
) -> JournalResponseEnvelope:
    if user.id != user_id:
        raise PermissionDeniedError()
    journals = journal_service.list_journals_by_user(user_id, limit, cursor)
    return JournalCursorEnvelope(data=JournalCursorResponse.from_journals(journals))


@router.get(
    "/search",
    response_model=JournalListResponseEnvelope,
    status_code=200,
    summary="일기 검색 (제목, 기간)",
    description="""
    다양한 검색 조건(쿼리 파라미터)을 조합하여 일기를 검색합니다.

    - 기간 조회: `start_date`와 `end_date`를 지정합니다. (예: `?start_date=...&end_date=...`)
    - 특정 날짜 조회: `start_date`와 `end_date`에 동일한 날짜를 지정합니다.
    - 제목과 조합: `title` 파라미터를 추가할 수 있습니다.
    """,
)
def search_journals(
    journal_service: Annotated[JournalService, Depends()],
    start_date: date | None = Query(
        None, description="조회 시작 날짜 (YYYY-MM-DD 형식)"
    ),
    end_date: date | None = Query(None, description="조회 종료 날짜 (YYYY-MM-DD 형식)"),
    title: str | None = Query(None, description="일기 제목"),
    user: User = Depends(get_current_user),
) -> JournalResponseEnvelope:
    if not any([title, start_date, end_date]):
        raise JournalBadRequestError("At least one query parameter must be provided.")
    if (start_date and not end_date) or (end_date and not start_date):
        raise JournalBadRequestError("Both start_date and end_date must be provided.")
    journals = journal_service.search_journals(
        user_id=user.id, start_date=start_date, end_date=end_date, title=title
    )
    return JournalListResponseEnvelope(data=JournalListResponse.from_journals(journals))


@router.patch(
    "/{journal_id}",
    response_model=JournalResponseEnvelope,
    status_code=200,
    summary="Update a journal entry by ID",
)
def update_journal_entry(
    journal_id: int,
    journal: JournalUpdateRequest,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> str:
    if journal_service.get_journal_owner(journal_id) != user.id:
        raise PermissionDeniedError()
    journal_service.update_journal(
        journal_id=journal_id,
        title=journal.title,
        content=journal.content,
        image_url=journal.image_url,
    )
    return "Update Success"


@router.delete(
    "/{journal_id}",
    response_model=JournalResponseEnvelope,
    status_code=200,
    summary="Delete a journal entry by ID",
)
def delete_journal_entry(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    user: User = Depends(get_current_user),
) -> str:
    if journal_service.get_journal_owner(journal_id) != user.id:
        raise PermissionDeniedError()
    journal_service.delete_journal(journal_id)
    return "Deletion Success"


@router.post(
    "/{journal_id}/image",
    response_model=PresignedUrlResponse,
    status_code=201,
    summary="Generate a presigned URL for image upload",
    description="This is async endpoint because it interacts with external services (AWS S3).",
)
async def generate_image_upload_url(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    payload: ImageUploadRequest,
    db: Session = Depends(get_db_session),
) -> PresignedUrlResponse:
    return await journal_service.create_image_presigned_url(
        db=db, journal_id=journal_id, payload=payload
    )


@router.post(
    "/{journal_id}/image/complete",
    response_model=JournalImageResponse,
    status_code=201,
    summary="Complete image upload",
    description="This endpoint is called to complete the image upload process; After client uploads the image to the presigned URL, it calls this endpoint to save the JournalImage record in DB.",
)
async def complete_image_upload(
    journal_id: int,
    journal_service: Annotated[JournalService, Depends()],
    s3_key: str = Query(..., description="The S3 key of the uploaded image"),
    db: Session = Depends(get_db_session),
) -> JournalImageResponse:
    return await journal_service.complete_image_upload(
        db=db, journal_id=journal_id, s3_key=s3_key
    )
