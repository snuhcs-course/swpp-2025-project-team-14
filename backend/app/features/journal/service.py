import json
import logging
from datetime import date
from functools import partial
from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.concurrency import run_in_threadpool
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI
from openai import AsyncOpenAI

from app.features.journal.errors import (
    ImageGenerationError,
    JournalBadRequestError,
    JournalNotFoundError,
    JournalUpdateError,
)
from app.features.journal.facade import JournalImageFacade
from app.features.journal.models import Journal, JournalImage, JournalKeyword
from app.features.journal.repository import JournalRepository
from app.features.journal.schemas.requests import (
    ImageCompletionRequest,
    ImageGenerateRequest,
    ImageUploadRequest,
)
from app.features.journal.schemas.responses import (
    JournalKeywordsListResponse,
    PresignedUrlResponse,
)
from app.features.journal.strategies import ImageStyleFactory, _load_prompt
from app.features.user.models import User

logger = logging.getLogger(__name__)


class JournalService:
    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
        image_facade: Annotated[JournalImageFacade, Depends()],
    ) -> None:
        self.journal_repository = journal_repository
        self.image_facade = image_facade

    def create_journal(
        self,
        user_id: int,
        title: str,
        content: str,
        emotions: dict[str, int],
        gratitude: str | None = None,
    ) -> Journal:
        return self.journal_repository.add_journal(
            user_id=user_id,
            title=title,
            content=content,
            emotions=emotions,
            gratitude=gratitude,
        )

    def get_journal(self, journal_id: int) -> Journal | None:
        journal = self.journal_repository.get_journal_by_id(journal_id)
        if journal is None:
            raise JournalNotFoundError(journal_id)
        return journal

    def delete_journal(self, journal_id: int) -> None:
        journal_to_delete = self.journal_repository.get_journal_by_id(journal_id)
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
        gratitude: str | None = None,
    ) -> None:
        if not any([title, content, gratitude]):
            raise JournalUpdateError()
        journal_to_update = self.journal_repository.get_journal_by_id(journal_id)
        if journal_to_update is None:
            raise JournalNotFoundError(journal_id)
        self.journal_repository.update_journal(
            journal=journal_to_update,
            title=title,
            content=content,
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
        limit: int = 10,
        cursor: int | None = None,
    ) -> list[Journal]:
        if start_date and end_date and start_date > end_date:
            raise JournalBadRequestError("start_date는 end_date보다 이전이어야 합니다.")
        return self.journal_repository.search_journals(
            user_id=user_id,
            title=title,
            start_date=start_date,
            end_date=end_date,
            limit=limit,
            cursor=cursor,
        )

    def get_journals_by_keyword(
        self,
        user_id: int,
        keyword: str,
        limit: int = 10,
        cursor: int | None = None,
    ) -> list[Journal]:
        return self.journal_repository.get_journals_by_keyword(
            user_id=user_id, keyword=keyword, limit=limit, cursor=cursor
        )

    async def create_image_presigned_url(
        self, journal_id: int, payload: ImageUploadRequest
    ) -> PresignedUrlResponse:
        return await self.image_facade.initiate_image_upload(
            journal_id, payload.filename, payload.content_type
        )

    async def complete_image_upload(
        self, journal_id: int, payload: ImageCompletionRequest
    ) -> JournalImage:
        return await self.image_facade.finalize_image_upload(journal_id, payload.s3_key)


class JournalOpenAIService:
    """
    OpenAI 관련 로직을 전담하는 서비스
    """

    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
        style_factory: Annotated[ImageStyleFactory, Depends()],
    ):
        self.journal_repository = journal_repository
        self.style_factory = style_factory

        keyword_prompt_text = _load_prompt("keyword_prompt.txt")

        self.client = AsyncOpenAI()
        if not self.client.api_key:
            raise ValueError("OPENAI_API_KEY not found.")

        self.structured_llm = ChatOpenAI(
            model="gpt-5-mini", temperature=0
        ).with_structured_output(JournalKeywordsListResponse)

        self.keyword_prompt_template = PromptTemplate.from_template(keyword_prompt_text)

    async def request_image_generation(
        self, request: ImageGenerateRequest, user: User
    ) -> str:
        """
        일기를 받아 이미지를 생성하고 Base64로 반환합니다.
        """
        strategy = self.style_factory.create_strategy(request.style)
        prompt_template = strategy.get_system_prompt()

        user_parts = [
            f"The protagonist of this diary is a {user.gender}, aged {user.age}."
        ]
        if user.appearance:
            user_parts.append(f"Appearance details: {user.appearance}.")
        user_description = " ".join(user_parts)

        try:
            prompt = await self._generate_scene_prompt_from_diary(
                request.content, prompt_template, user_description
            )
        except Exception as e:
            logger.error(
                f"GPT call failed during scene prompt generation: {e}", exc_info=True
            )
            raise ImageGenerationError(
                "Failed to generate scene description from diary."
            ) from e

        try:
            image_b64 = await self._generate_image_from_prompt(prompt)
        except Exception as e:
            logger.error(f"DALL-E 3 call failed: {e}", exc_info=True)
            raise ImageGenerationError(
                "Failed to create image from the description."
            ) from e

        return image_b64

    async def _generate_scene_prompt_from_diary(
        self, journal_content: str, input_prompt: str, user_description: str
    ) -> str:
        """
        (Helper) GPT-5-mini를 비동기 호출하여 DALL-E용 프롬프트를 생성합니다.
        """
        system_prompt = (
            f"{input_prompt}\n\n"
            f"[Character Consistency Requirement]\n"
            f"{user_description}\n"
            f"You MUST ensure the person in the generated scene prompt matches the description above."
        )
        chat_response = await self.client.chat.completions.create(
            model="gpt-5-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": journal_content},
            ],
            response_format={"type": "json_object"},
        )
        scene_data = json.loads(chat_response.choices[0].message.content)
        return scene_data["scene_prompt"]

    async def _generate_image_from_prompt(self, prompt: str) -> str:
        """
        (Helper) DALL-E 3를 비동기 호출하여 Base64 이미지 데이터를 반환합니다.
        """
        image_response = await self.client.images.generate(
            model="dall-e-3",
            prompt=prompt,
            size="1024x1024",
            quality="standard",
            n=1,
            response_format="b64_json",
        )
        return image_response.data[0].b64_json

    async def extract_keywords_with_emotion_associations(
        self, journal_id: int
    ) -> list[JournalKeyword]:
        find_journal = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal)
        if not journal:
            raise JournalNotFoundError(journal_id)

        emotion_names = [e.emotion for e in journal.emotions]
        if not emotion_names:
            raise JournalBadRequestError("No emotion in journal")

        chain = self.keyword_prompt_template | self.structured_llm

        input_data = {
            "content": journal.content,
            "emotion_names": ", ".join(emotion_names),
        }
        res = None
        try:
            res_envelope = await chain.ainvoke(input_data)
            res = res_envelope.data
        except Exception as e:
            logger.error(
                f"LangChain(ainvoke) call failed for journal {journal_id}: {e}",
                exc_info=True,
            )
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to extract keywords.",
            ) from e

        if not res:
            logger.warning(f"LLM returned empty result for journal {journal_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="LLM returned invalid format.",
            )

        unique_res = []
        set_keywords = set()

        for item in res:
            normalized_keyword = item.keyword.strip()
            normalized_keyword = normalized_keyword.lower()

            if normalized_keyword not in set_keywords:
                set_keywords.add(normalized_keyword)
                item.keyword = normalized_keyword
                unique_res.append(item)

        save_task = partial(
            self.journal_repository.add_keywords_emotion_associations,
            journal_id=journal_id,
            keyword_emotion_associations=res,
        )
        created_keywords = await run_in_threadpool(save_task)

        return created_keywords
