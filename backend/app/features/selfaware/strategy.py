from __future__ import annotations

import random
from abc import ABC, abstractmethod

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from app.features.journal.repository import JournalRepository
from app.features.selfaware.models import Question
from app.features.selfaware.prompt import (
    CATEGORIES,
    CategoryExtractionResponse,
    JournalSummary,
    QuestionGenerationResponse,
    category_prompt,
    multi_category_prompt,
    personalized_prompt,
    single_category_prompt,
    summary_prompt,
)
from app.features.selfaware.repository import QuestionRepository


class QuestionStrategy(ABC):
    @abstractmethod
    def generate(
        self,
        user_id: int,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ) -> Question:
        pass


class SelfawareStrategy(QuestionStrategy):
    def generate(
        self,
        user_id: int,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ):
        llm = ChatOpenAI(model="gpt-5-nano")
        summary_parser = PydanticOutputParser(pydantic_object=JournalSummary)
        category_parser = PydanticOutputParser(
            pydantic_object=CategoryExtractionResponse
        )
        question_parser = PydanticOutputParser(
            pydantic_object=QuestionGenerationResponse
        )

        # ✅ 1. 유저의 일기 가져오기
        journals = journal_repository.list_journals_by_user(user_id, 5)

        # ✅ 2. 최근 일기 3~5개만 선택 (토큰 제한 방지)
        combined_content = "\n".join([f"- {j.content}" for j in journals])

        summary_chain = summary_prompt | llm | summary_parser

        summary_response = summary_chain.invoke(
            {
                "journal_text": combined_content,
            }
        )
        summary = summary_response.summary

        # ✅ 4. 감정 분석 및 관련 카테고리 추출
        category_chain = category_prompt | llm | category_parser
        category_response: CategoryExtractionResponse = category_chain.invoke(
            {"summary": summary}
        )

        analysis = category_response.analysis
        categories = category_response.categories
        categories_text_list = []
        for category_en, category_ko in categories:
            categories_text_list.append(f"{category_en}({category_ko})")
        categories_text = ", ".join(categories_text_list)

        # ✅ 5. 분석 결과를 바탕으로 자기성찰 질문 생성
        question_chain = personalized_prompt | llm | question_parser
        response: QuestionGenerationResponse = question_chain.invoke(
            {"summary": summary, "analysis": analysis, "categories": categories_text}
        )

        # DB에 저장
        question = question_repository.create_question(
            user_id=user_id,
            question_type="personalized_category",
            text=response.question,
        )

        return question


class SingleStrategy(QuestionStrategy):
    def generate(
        self,
        user_id: int,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ):
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = PydanticOutputParser(pydantic_object=QuestionGenerationResponse)

        # 랜덤하게 카테고리 선택
        selected_category = random.choice(CATEGORIES)
        category_en = selected_category[0]
        category_ko = selected_category[1]
        category = f"{category_en}({category_ko})"

        single_category_chain = single_category_prompt | llm | output_parser
        response: QuestionGenerationResponse = single_category_chain.invoke(
            {"category": category}
        )

        # DB에 저장
        question = question_repository.create_question(
            user_id=user_id,
            question_type="single_category",
            text=response.question,
        )

        return question


class MultiStrategy(QuestionStrategy):
    def generate(
        self,
        user_id: int,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ):
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = PydanticOutputParser(pydantic_object=QuestionGenerationResponse)

        # 랜덤하게 2-3개 카테고리 선택
        num_categories = random.randint(2, 3)
        selected_categories = random.sample(CATEGORIES, num_categories)

        categories_text = []
        for category_en, category_ko in selected_categories:
            categories_text.append(f"{category_en}({category_ko})")

        categories = ", ".join(categories_text)

        multi_category_chain = multi_category_prompt | llm | output_parser
        response: QuestionGenerationResponse = multi_category_chain.invoke(
            {"categories": categories}
        )

        # DB에 저장
        question = question_repository.create_question(
            user_id=user_id,
            question_type="multi_category",
            text=response.question,
        )

        return question


class NavigationContext:
    def __init__(self):
        self.question_strategy: QuestionStrategy | None = None  # None 명시

    def set_question_strategy(self, question_strategy: QuestionStrategy):
        self.question_strategy = question_strategy

    def perform(self, user_id, journal_repository, question_repository):
        if self.question_strategy is None:
            raise ValueError("Question strategy is not set.")
        return self.question_strategy.generate(
            user_id, journal_repository, question_repository
        )


Strategies = [SelfawareStrategy(), SingleStrategy(), MultiStrategy()]
