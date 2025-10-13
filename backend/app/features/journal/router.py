from typing import Annotated

from fastapi import APIRouter, Depends, Query
from fastapi.security import HTTPBearer

from app.common.authorization import get_current_user
from app.common.errors import PermissionDeniedError
from app.features.journal.schemas.requests import (
    JournalCreateRequest,
    JournalUpdateRequest,
)
from app.features.journal.schemas.responses import (
    JournalCursorEnvelope,
    JournalCursorResponse,
    JournalResponse,
    JournalResponseEnvelope,
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
