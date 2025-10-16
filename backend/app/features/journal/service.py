import os
import uuid
from datetime import date
from functools import partial
from typing import Annotated

from fastapi import Depends, HTTPException, UploadFile, status
from fastapi.concurrency import run_in_threadpool

from app.features.journal.errors import (
    ImageUploadError,
    JournalBadRequestError,
    JournalNotFoundError,
    JournalUpdateError,
)
from app.features.journal.models import Journal, JournalImage
from app.features.journal.repository import (
    ImageGenerationRepository,
    JournalRepository,
    S3Repository,
)
from app.features.journal.schemas.requests import (
    ImageCompletionRequest,
    ImageGenerateRequest,
    ImageGenerateResponse,
    ImageUploadRequest,
)
from app.features.journal.schemas.responses import PresignedUrlResponse


class JournalService:
    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
        s3_repository: Annotated[S3Repository, Depends()],
        image_generation_repository: Annotated[ImageGenerationRepository, Depends()],
    ) -> None:
        self.journal_repository = journal_repository
        self.s3_repository = s3_repository
        self.image_generation_repository = image_generation_repository

    def create_journal(
        self,
        user_id: int,
        title: str,
        content: str,
        emotions: dict[str, int],
        image_urls: list[str] | None = None,
    ) -> Journal:
        return self.journal_repository.add_journal(
            user_id=user_id,
            title=title,
            content=content,
            emotions=emotions,
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

    # image upload via S3 presigned URL
    async def create_image_presigned_url(
        self, journal_id: int, payload: ImageUploadRequest
    ) -> PresignedUrlResponse:
        """클라이언트가 이미지 업로드를 시작할 때 호출됩니다."""
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal_task)

        if not journal:
            raise JournalNotFoundError(journal_id)

        # 고유 파일 키 생성
        _, file_extension = os.path.splitext(payload.filename)
        unique_filename = f"{uuid.uuid4()}{file_extension}"
        s3_key = f"images/journals/{journal_id}/{unique_filename}"

        # 비동기 S3 작업 실행
        url_data = await self.s3_repository.generate_upload_url(
            s3_key, payload.content_type
        )
        if not url_data:
            raise ImageUploadError("Could not generate S3 presigned URL.")

        return PresignedUrlResponse(**url_data, s3_key=s3_key)

    async def complete_image_upload(
        self, journal_id: int, payload: ImageCompletionRequest
    ) -> JournalImage:
        """클라이언트가 이미지 업로드 완료를 알릴 때 호출됩니다."""
        file_exists = await self.s3_repository.check_file_exists(payload.s3_key)
        if not file_exists:
            raise ImageUploadError("Uploaded image not found in S3.")
        # 동기 DB 작업을 스레드 풀에서 실행
        create_image_task = partial(
            self.journal_repository.create_journal_image,
            journal_id=journal_id,
            image_url=payload.image_url,
        )

        journal_image = await run_in_threadpool(create_image_task)
        return journal_image

    # AI image generation
    async def request_image_generation(
        self, request: ImageGenerateRequest
    ) -> ImageGenerateResponse:
        """AI 이미지 생성을 요청하고, Job ID를 반환합니다."""
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id,
            journal_id=request.journal_id,
        )
        journal = await run_in_threadpool(find_journal_task)
        if not journal:
            raise JournalNotFoundError(request.journal_id)

        sd_response = await self.image_generation_repository.enqueue_image_generation(
            prompt=request.prompt_text, journal_id=request.journal_id
        )
        job_id = sd_response.get("job_id")
        if not job_id:
            raise HTTPException(
                status.HTTP_500_INTERNAL_SERVER_ERROR,
                "Failed to get job_id from SD API.",
            )

        create_job_task = partial(
            self.journal_repository.create_image_generation_job,
            journal_id=request.journal_id,
            job_id=job_id,
        )
        await run_in_threadpool(create_job_task)

        return ImageGenerateResponse(job_id=job_id, status="queued")

    async def process_image_generation_webhook(
        self, job_id: str, journal_id: int, image_file: UploadFile
    ):
        """AI 이미지 생성이 완료되었을 때 호출되는 Webhook 핸들러입니다."""
        unique_filename = f"generated-images/{journal_id}/{uuid.uuid4()}.png"
        s3_url_data = await self.s3_repository.upload_file_object(
            file_obj=image_file.file,
            s3_key=unique_filename,
            content_type=image_file.content_type,
        )
        if not s3_url_data:
            raise HTTPException(
                status.HTTP_500_INTERNAL_SERVER_ERROR, "Failed to upload image to S3."
            )

        update_task = partial(
            self.journal_repository.update_image_generation_url_by_job_id,
            job_id=job_id,
            image_url=s3_url_data["file_url"],
        )
        updated_image = await run_in_threadpool(update_task)
        if not updated_image:
            raise HTTPException(
                status.HTTP_404_NOT_FOUND, f"Job ID {job_id} not found."
            )

        return updated_image
