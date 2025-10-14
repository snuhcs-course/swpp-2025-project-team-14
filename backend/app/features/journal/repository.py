from datetime import date, datetime, timedelta
from typing import Annotated

from fastapi import Depends
from sqlalchemy.orm import Session

from app.database.session import get_db_session

from .models import Journal, JournalImage


class JournalRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def add_journal(
        self,
        user_id: int,
        title: str,
        content: str,
        image_urls: list[str] | None = None,
    ) -> Journal:
        journal = Journal(
            user_id=user_id,
            title=title,
            content=content,
        )

        if image_urls:
            for image_url in image_urls:
                journal_image = JournalImage(journal_id=journal.id, image_url=image_url)
                journal.images.append(journal_image)

        self.session.add(journal)
        self.session.flush()
        return journal

    def get_journal_by_id(self, journal_id: int) -> Journal | None:
        return self.session.get(Journal, journal_id)

    def delete_journal(self, journal: Journal) -> None:
        self.session.delete(journal)

    def list_journals_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Journal]:
        # cursor가 None이면 최신 글부터, cursor가 주어지면 해당 ID보다 작은 글부터
        query = (
            self.session.query(Journal)
            .filter(Journal.user_id == user_id)
            .order_by(Journal.id.desc())
        )
        # cursor가 주어지면 해당 ID보다 작은 글부터
        if cursor is not None:
            query = query.filter(Journal.id < cursor)
        # limit만큼 가져오기
        return query.limit(limit).all()

    def update_journal(
        self,
        journal: Journal,
        title: str | None = None,
        content: str | None = None,
        summary: str | None = None,
        gratitude: str | None = None,
    ) -> None:
        if title is not None:
            journal.title = title
        if content is not None:
            journal.content = content
        if summary is not None:
            journal.summary = summary
        if gratitude is not None:
            journal.gratitude = gratitude
        self.session.flush()

    def search_journals(
        self,
        user_id: int,
        title: str | None = None,
        start_date: date | None = None,
        end_date: date | None = None,
    ) -> list[Journal]:
        query = self.session.query(Journal).filter(Journal.user_id == user_id)

        if title:
            query = query.filter(Journal.title.ilike(f"%{title}%"))
        if start_date:
            query = query.filter(
                Journal.created_at >= datetime.combine(start_date, datetime.min.time())
            )
        if end_date:
            query = query.filter(
                Journal.created_at
                < datetime.combine(end_date + timedelta(days=1), datetime.min.time())
            )

        return query.order_by(Journal.created_at.desc()).all()
