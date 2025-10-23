from __future__ import annotations
from typing import Annotated, List, Optional, Literal, Dict, Any, Tuple
from fastapi import Depends
from datetime import datetime, timezone
import random
import json

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from app.core.config import settings
from dotenv import load_dotenv
load_dotenv()

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser, PydanticOutputParser
from langchain.schema.runnable import RunnableMap

from .prompt import emotion_prompt, question_prompt, single_category_prompt, multi_category_prompt, value_score_prompt
from .repository import JournalRepository, QuestionRepository, AnswerRepository, ValueMapRepository, ValueScoreRepository
from .schemas.responses import QuestionCreate, Question, AnswerCreate, ValueMapCreate, ValueScoreCreate
from value_map import analyze_personality

class QuestionService:
    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
        question_repository: Annotated[QuestionRepository, Depends()],
    ) -> None:
        self.journal_repository = journal_repository
        self.question_repository = question_repository


    def generate_selfaware_question(self, user_id: int):
        """
        특정 사용자의 일기를 DB에서 불러와 요약 → 자기성찰 질문을 생성 → DB 저장
        """
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = StrOutputParser()

        # ✅ 1. 유저의 일기 가져오기
        journals = self.journal_repository.list_journals_by_user(user_id)
        if not journals or len(journals) == 0:
            raise ValueError(f"User {user_id} has no journal entries.")

        # ✅ 2. 최근 일기 3~5개만 선택 (토큰 제한 방지)
        recent_journals = journals[-5:]
        combined_content = "\n".join([f"- {j.content}" for j in recent_journals])

        # ✅ 3. 일기 요약 (너무 길면 LLM 부담됨)
        summary_prompt = ChatPromptTemplate.from_template(
            "다음은 한 사용자의 최근 일기 내용입니다. 이 일기들의 전반적인 감정과 주제를 간결하게 요약해 주세요.\n\n{journal_text}"
        )
        summary_chain = summary_prompt | llm | StrOutputParser()
        journal_summary = summary_chain.invoke({"journal_text": combined_content})

        # ✅ 4. 감정 분석 → 자기성찰 질문 생성
        emotion_chain = emotion_prompt | llm
        question_chain = question_prompt | llm

        reflection_agent = RunnableMap({
            "analysis": emotion_chain,
        }) | question_chain | output_parser

        response = reflection_agent.invoke({"journal": journal_summary})

        # DB에 저장
        question_data = QuestionCreate(
            user_id=user_id,
            text=response,
            type="personalized_category",
        )

        question = self.question_repository.create(question_data)

        return question
    
    def generate_single_category_question(self, user_id: int):
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = StrOutputParser()

        # ✅ 1. 유저의 일기 가져오기
        journals = self.journal_repository.list_journals_by_user(user_id)
        if not journals or len(journals) == 0:
            raise ValueError(f"User {user_id} has no journal entries.")

        # ✅ 2. 최근 일기 선택 
        recent_journal = journals[-1]

        # ✅ 3. 유사 카테고리 선택
        category_prompt = ChatPromptTemplate.from_template(
            "다음은 한 사용자의 최근 일기 내용입니다. '성장과 자기실현','관계와 연결','안정과 안전','자유와 자율','성취와 영향력','즐거움과 만족', '윤리와 초월' 중 일기의 내용과 가장 가까운 것을 선택하여 출력해주세요.\n\n{journal_text}"
        )
        category_chain = category_prompt | llm | StrOutputParser()
        category = category_chain.invoke({"journal_text": recent_journal})

        single_category_chain = single_category_prompt | llm | output_parser
        response = single_category_chain.invoke({"cat": category})

        # DB에 저장
        question_data = QuestionCreate(
            user_id=user_id,
            text=response,
            type="single_category",
        )

        question = self.question_repository.create(question_data)

        return question

    def generate_multi_category_question(self, user_id: int):
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = StrOutputParser()

        # ✅ 1. 유저의 일기 가져오기
        journals = self.journal_repository.list_journals_by_user(user_id)
        if not journals or len(journals) == 0:
            raise ValueError(f"User {user_id} has no journal entries.")

        # ✅ 2. 최근 일기 선택 
        recent_journal = journals[-1]

        # ✅ 3. 유사 카테고리 선택
        categories_prompt = ChatPromptTemplate.from_template(
            "다음은 한 사용자의 최근 일기 내용입니다. '성장과 자기실현','관계와 연결','안정과 안전','자유와 자율','성취와 영향력','즐거움과 만족', '윤리와 초월' 중 일기의 내용과 가까운 것을 2가지 내지 3가지 선택하여 출력해주세요.\n\n{journal_text}"
        )
        categories_chain = categories_prompt | llm | StrOutputParser()
        categories = categories_chain.invoke({"journal_text": recent_journal})

        multi_category_chain = multi_category_prompt | llm | output_parser
        response = multi_category_chain.invoke({"cats": categories})

        # DB에 저장
        question_data = QuestionCreate(
            user_id=user_id,
            text=response,
            type="multi_category",
        )

        question = self.question_repository.create(question_data)

        return question

    def get_questions_by_id(self, question_id: int):
        return self.question_repository.get(question_id)


    def get_questions_by_user(self, user_id: int):
        return self.question_repository.get_by_user(user_id)


class AnswerService:
    def __init__(
        self,
        question_repository: Annotated[QuestionRepository, Depends()],
        answer_repository: Annotated[AnswerRepository, Depends()],
    ) -> None:
        self.question_repository = question_repository
        self.answer_repository = answer_repository


    def create_answer(self, text: str, type: str | None, keywords: str | None, user_id: int, question_id: int,):
        """
        해당 question이 실제 존재하는지 확인한 뒤,
        answer를 DB에 생성한다.
        """
        question = self.question_repository.get(question_id)
        if not question:
            raise ValueError(f"Question(id={question_id})이 존재하지 않습니다.")
        answer_data = AnswerCreate(text = text,
                                    type = type,
                                    keywords = keywords, 
                                    user_id = user_id, 
                                    question_id=question_id)
        return self.answer_repository.create(answer_data)


    def get_answers_by_question(self, question_id: int):
        return self.answer_repository.get_by_question(question_id)


    def get_answers_by_user(self, user_id: int):
        return self.answer_repository.get_by_user(user_id)

class ValueScoreService:
    def __init__(
        self,
        question_repository: Annotated[QuestionRepository, Depends()],
        answer_repository: Annotated[AnswerRepository, Depends()],
        value_score_repository: Annotated[ValueScoreRepository, Depends()],
    ) -> None:
        self.question_repository = question_repository
        self.answer_repository = answer_repository
        self.value_score_repository = value_score_repository

    def get_value_score_from_answer(self, user_id:int, question_id:int, answer_id: int):
        llm = ChatOpenAI(model="gpt-5-nano")

        question = self.question_repository.get(question_id)
        answer = self.answer_repository.get(answer_id)
        value_score_chain = value_score_prompt | llm
        response = value_score_chain.invoke({"question": question, "answer":answer})
        content = response.content if hasattr(response, "content") else str(response)

        try:
            data = json.loads(content) # type: ignore
            detected_values = data.get("detected_values", [])

            # 예시: 개별 값 변수로 접근
            for v in detected_values:
                value_score_data = ValueScoreCreate(
                    answer_id = answer_id,
                    user_id = user_id,
                    category = v['category_key'],
                    value = v['value_name'],
                    confidence= v['confidence'],
                    intensity= v['intensity'],
                    polarity= v['polarity'],
                )
                self.value_score_repository.create(value_score_data)
        except json.JSONDecodeError:
            print("❌ JSON 파싱 실패. 모델의 출력 형식을 확인하세요.")
            detected_values = []
        
        return detected_values

class ValueMapService:
    def __init__(
        self,
        question_repository: Annotated[QuestionRepository, Depends()],
        answer_repository: Annotated[AnswerRepository, Depends()],
        value_map_repository: Annotated[ValueMapRepository, Depends()],
    ) -> None:
        self.question_repository = question_repository
        self.answer_repository = answer_repository
        self.value_map_repository = value_map_repository


    def analyze_user_personality(self, user_id: int):
        # 1. 유저의 질문과 답변을 가져오기
        questions = self.question_repository.get_by_user(user_id)
        qa_pairs = []

        if not questions:
            raise ValueError(f"User {user_id} has no question entries.")
        
        for q in questions:
            answers = self.answer_repository.get_by_question(q.id)
            for a in answers:
                qa_pairs.append({"question": q.text, "answer": a.text})

        if not qa_pairs:
            raise ValueError(f"User {user_id} has no answer entries.")

        result = analyze_personality(qa_pairs)

        value_map_data = ValueMapCreate(user_id=user_id, value_map=result)
        saved_map = self.value_map_repository.create(value_map_data)

        return {"user_id": user_id, "values": saved_map.value_map, "created_at": saved_map.created_at}