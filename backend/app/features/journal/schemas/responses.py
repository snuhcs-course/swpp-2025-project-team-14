from datetime import datetime

from pydantic import BaseModel, Field

from app.common.schemas import ResponseEnvelope
from app.features.journal.models import Journal, JournalImage, JournalKeyword


class KeywordEmotionAssociationItem(BaseModel):
    keyword: str
    emotion: str
    weight: float = Field(..., ge=0.0, le=1.0, description="association (0..1)")

    @staticmethod
    def from_journal_keyword(
        journal_keyword: JournalKeyword,
    ) -> "KeywordEmotionAssociationItem":
        return KeywordEmotionAssociationItem(
            keyword=journal_keyword.keyword,
            emotion=journal_keyword.emotion,
            weight=journal_keyword.weight,
        )


class JournalKeywordListResponseEnvelope(ResponseEnvelope):
    data: list[KeywordEmotionAssociationItem]


class JournalEmotionResponse(BaseModel):
    emotion: str
    intensity: int


class JournalResponse(BaseModel):
    id: int
    title: str
    content: str
    emotions: list[JournalEmotionResponse]
    keywords: list[KeywordEmotionAssociationItem]
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
            emotions=[
                JournalEmotionResponse(
                    emotion=emotion.emotion, intensity=emotion.intensity
                )
                for emotion in journal.emotions
            ],
            keywords=[
                KeywordEmotionAssociationItem.from_journal_keyword(keyword)
                for keyword in journal.keywords
            ],
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


class JournalImageResponse(BaseModel):
    id: int
    journal_id: int
    s3_key: str
    created_at: datetime

    @staticmethod
    def from_journal_image(journal_image: JournalImage) -> "JournalImageResponse":
        return JournalImageResponse(
            id=journal_image.id,
            journal_id=journal_image.journal_id,
            s3_key=journal_image.s3_key,
            created_at=journal_image.created_at,
        )


class JournalImageResponseEnvelope(ResponseEnvelope):
    data: JournalImageResponse
