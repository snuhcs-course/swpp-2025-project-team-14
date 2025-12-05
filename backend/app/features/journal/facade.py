import os
import uuid
from functools import partial
from typing import Annotated

from fastapi import Depends
from fastapi.concurrency import run_in_threadpool

from app.features.journal.errors import ImageUploadError, JournalNotFoundError
from app.features.journal.models import JournalImage
from app.features.journal.repository import JournalRepository, S3Repository
from app.features.journal.schemas.responses import PresignedUrlResponse


class JournalImageFacade:
    """
    Handles journal image flow across S3, DB, and file handling.
    Provides a simplified facade API for consumers.
    """

    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
        s3_repository: Annotated[S3Repository, Depends()],
    ) -> None:
        self.journal_repository = journal_repository
        self.s3_repository = s3_repository

    async def initiate_image_upload(
        self, journal_id: int, filename: str, content_type: str
    ) -> PresignedUrlResponse:
        """Upload initiation: generate UUID filename and presigned URL."""
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal_task)

        if not journal:
            raise JournalNotFoundError(journal_id)

        # Create unique file key
        _, file_extension = os.path.splitext(filename)
        unique_filename = f"{uuid.uuid4()}{file_extension}"
        s3_key = f"images/journals/{journal_id}/{unique_filename}"

        # Build S3 upload URL
        url_data = await self.s3_repository.generate_upload_url(s3_key, content_type)
        if not url_data:
            raise ImageUploadError("Could not generate S3 presigned URL.")

        return PresignedUrlResponse(**url_data, s3_key=s3_key)

    async def finalize_image_upload(self, journal_id: int, s3_key: str) -> JournalImage:
        """업로드 완료: S3 확인 및 DB 갱신"""
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal_task)
        if not journal:
            raise JournalNotFoundError(journal_id)

        file_exists = await self.s3_repository.check_file_exists(s3_key)
        if not file_exists:
            raise ImageUploadError("Uploaded image not found in S3.")

        existing = await run_in_threadpool(
            self.journal_repository.get_image_by_journal_id, journal_id
        )

        replace_task = partial(
            self.journal_repository.replace_journal_image,
            journal_id=journal_id,
            existing_image=existing,
            s3_key=s3_key,
        )
        journal_image = await run_in_threadpool(replace_task)

        if existing and existing.s3_key:
            await self.s3_repository.delete_object(existing.s3_key)
        return journal_image
