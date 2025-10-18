import os
import uuid
from datetime import date
from functools import partial
from typing import Annotated

from fastapi import Depends, HTTPException, UploadFile, status
from fastapi.concurrency import run_in_threadpool
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI

from app.features.journal.errors import (
    ImageUploadError,
    JournalBadRequestError,
    JournalNotFoundError,
    JournalUpdateError,
)
from app.features.journal.models import Journal, JournalImage, JournalKeyword
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
from app.features.journal.schemas.responses import (
    JournalKeywordListResponseEnvelope,
    PresignedUrlResponse,
)


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
        summary: str | None = None,
        gratitude: str | None = None,
    ) -> None:
        if not any([title, content, summary, gratitude]):
            raise JournalUpdateError()
        journal_to_update = self.journal_repository.get_journal_by_id(journal_id)
        if journal_to_update is None:
            raise JournalNotFoundError(journal_id)
        self.journal_repository.update_journal(
            journal=journal_to_update,
            title=title,
            content=content,
            summary=summary,
            gratitude=gratitude,
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

        # 저널 존재 확인
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal_task)
        if not journal:
            raise JournalNotFoundError(journal_id)

        # 기존 업로드 이미지가 있으면 S3에서 삭제
        existing = await run_in_threadpool(
            self.journal_repository.get_image_by_journal_and_type,
            journal_id,
            "uploaded",
        )
        if existing and existing.s3_key:
            # S3에서 삭제
            await self.s3_repository.delete_object(existing.s3_key)

        # DB에서 교체 (replace_journal_image) — image_type='uploaded'
        replace_task = partial(
            self.journal_repository.replace_journal_image,
            journal_id=journal_id,
            existing_image=existing,
            s3_key=payload.s3_key,
            job_id=None,
            image_type="uploaded",
        )
        journal_image = await run_in_threadpool(replace_task)
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

        response = await self.image_generation_repository.enqueue_image_generation(
            prompt=request.prompt_text, journal_id=request.journal_id
        )
        job_id = response.get("job_id")
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
        """AI 이미지 생성 완료 webhook 처리: S3 업로드 후 DB에 교체"""
        s3_key = f"generated-images/{journal_id}/{uuid.uuid4()}.png"
        s3_url_data = await self.s3_repository.upload_file_object(
            file_obj=image_file.file,
            s3_key=s3_key,
            content_type=image_file.content_type,
        )
        if not s3_url_data:
            raise HTTPException(
                status.HTTP_500_INTERNAL_SERVER_ERROR, "Failed to upload image to S3."
            )

        # 기존 생성 이미지가 있으면 S3에서 삭제 (기존 image_type='generated' 레코드 찾기)
        existing_generated = await run_in_threadpool(
            self.journal_repository.get_image_by_journal_and_type,
            journal_id,
            "generated",
        )
        if existing_generated and existing_generated.s3_key:
            # S3에서 삭제
            await self.s3_repository.delete_object(existing_generated.s3_key)

        # DB에 새 생성 이미지로 교체 (image_type='generated')
        replace_task = partial(
            self.journal_repository.replace_journal_image,
            journal_id=journal_id,
            existing_image=existing_generated,
            s3_key=s3_key,
            job_id=None,
            image_type="generated",
        )
        updated_image = await run_in_threadpool(replace_task)

        if not updated_image:
            raise HTTPException(
                status.HTTP_404_NOT_FOUND,
                f"Failed to update image record for job {job_id}.",
            )

        return updated_image

    async def extract_keywords_with_emotion_associations(
        self, journal_id: int
    ) -> list[JournalKeyword]:
        """
        - journal 내용으로부터 키워드 추출
        - 각 키워드에 대해 journal에 존재하는 감정 목록과의 연관도(0~1) 산출
        - DB에 JournalKeyword 저장
        """
        # DB에서 journal 불러오기 (스레드풀)
        find_journal = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal)
        if not journal:
            raise JournalNotFoundError(journal_id)

        # 감정 목록 준비
        emotion_names = [e.emotion for e in journal.emotions]
        if not emotion_names:
            raise JournalBadRequestError("저널에 감정 데이터가 없습니다.")

        # journal 내용 가져오기
        content = journal.content

        prompt_text = """
        You are given a journal content and a list of emotion labels.
        Extract up to 10 meaningful keywords from the content.
        For each keyword, provide a mapping of provided emotion to a float between 0 and 1 indicating association strength.
        You should only include the emotion with the highest association strength for each keyword.
        Also, Do not extract emotion, feeling, or subjective words as keywords. Do not extract duplicate keywords, either. There should be no overlap between keywords.
        Return ONLY valid JSON matching the schema.

        Journal content:
        {content}

        Emotions:
        {emotion_names}
        """
        prompt = PromptTemplate.from_template(prompt_text)
        llm = ChatOpenAI(model="gpt-5-nano", temperature=0).with_structured_output(
            JournalKeywordListResponseEnvelope
        )

        # LangChain 체인 구성 및 실행
        chain = prompt | llm

        # blocking이므로 스레드풀에서 실행
        res = await run_in_threadpool(chain.invoke, content, ", ".join(emotion_names))

        if not res:
            raise HTTPException(
                status.HTTP_500_INTERNAL_SERVER_ERROR,
                "Failed to get response from LLM.",
            )

        # DB에 저장 (스레드풀에서 repository 호출)
        save_task = partial(
            self.journal_repository.add_keywords_emotion_associations,
            journal_id=journal_id,
            keyword_emotion_associations=res,
        )
        created_keywords = await run_in_threadpool(save_task)

        return created_keywords
