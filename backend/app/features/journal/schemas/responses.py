from datetime import datetime

from pydantic import BaseModel, Field

from app.common.schemas import ResponseEnvelope
from app.features.journal.models import Journal


class JournalResponse(BaseModel):
    id: int
    title: str
    content: str
    image_urls: list[str] | None = None
    summary: str | None = None
    gratitude: str | None = None
    created_at: datetime

    @staticmethod
    def from_journal(journal: Journal) -> "JournalResponse":
        return JournalResponse(
            id=journal.id,
            title=journal.title,
            content=journal.content,
            image_urls=[image.image_url for image in journal.images]
            if journal.images
            else None,
            summary=journal.summary,
            gratitude=journal.gratitude,
            created_at=journal.created_at,
        )


class JournalResponseEnvelope(ResponseEnvelope):
    data: JournalResponse


class JournalCursorResponse(BaseModel):
    items: list[JournalResponse]
    next_cursor: int | None = Field(
        None, description="다음 페이지를 요청할 때 사용할 마지막 아이템의 ID"
    )

    @staticmethod
    def from_journals(journals: list[Journal]) -> "JournalCursorResponse":
        items = [JournalResponse.from_journal(journal) for journal in journals]
        next_cursor = items[-1].id if items else None
        return JournalCursorResponse(items=items, next_cursor=next_cursor)


class JournalCursorEnvelope(ResponseEnvelope):
    data: JournalCursorResponse


class JournalListResponse(BaseModel):
    data: list[JournalResponse]

    @staticmethod
    def from_journals(journals: list[Journal]) -> "JournalListResponse":
        items = [JournalResponse.from_journal(journal) for journal in journals]
        return JournalListResponse(data=items)


class JournalListResponseEnvelope(ResponseEnvelope):
    data: JournalListResponse


class PresignedUrlResponse(BaseModel):
    presigned_url: str
    file_url: str
    s3_key: str
