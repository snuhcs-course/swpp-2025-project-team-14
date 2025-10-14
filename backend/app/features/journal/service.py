from datetime import date
from typing import Annotated

from fastapi import Depends

from app.features.journal.errors import (
    JournalBadRequestError,
    JournalNotFoundError,
    JournalUpdateError,
)
from app.features.journal.models import Journal
from app.features.journal.repository import JournalRepository


class JournalService:
    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
    ) -> None:
        self.journal_repository = journal_repository

    def create_journal(
        self,
        user_id: int,
        title: str,
        content: str,
        image_urls: list[str] | None = None,
    ) -> Journal:
        return self.journal_repository.add_journal(
            user_id=user_id,
            title=title,
            content=content,
            image_urls=image_urls,
        )

    def get_journal(self, journal_id: int) -> Journal | None:
        return self.journal_repository.get_journal_by_id(journal_id)

    def delete_journal(self, journal: Journal) -> None:
        journal_to_delete = self.journal_repository.get_journal_by_id(journal.id)
        self.journal_repository.delete_journal(journal_to_delete)

    def list_journals_by_user(
        self, user_id: int, limit: int, cursor: int | None = None
    ) -> list[Journal]:
        return self.journal_repository.list_journals_by_user(user_id, limit, cursor)

    def update_journal(
        self,
        journal_id: int,
        title: str | None = None,
        content: str | None = None,
        image_url: str | None = None,
    ) -> None:
        if title is None and content is None and image_url is None:
            raise JournalUpdateError()
        journal_to_update = self.journal_repository.get_journal_by_id(journal_id)
        if journal_to_update is None:
            raise JournalNotFoundError(journal_id)
        return self.journal_repository.update_journal(
            journal=journal_to_update,
            title=title,
            content=content,
            image_url=image_url,
        )

    def get_journal_owner(self, journal_id: int) -> int | None:
        journal = self.journal_repository.get_journal_by_id(journal_id)
        if journal is None:
            return None
        return journal.user_id

    def search_journals(
        self,
        user_id: int,
        title: str | None = None,
        start_date: date | None = None,
        end_date: date | None = None,
    ) -> list[Journal]:
        if start_date and end_date and start_date > end_date:
            raise JournalBadRequestError("start_date는 end_date보다 이전이어야 합니다.")
        return self.journal_repository.search_journals(
            user_id=user_id, title=title, start_date=start_date, end_date=end_date
        )
