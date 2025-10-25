from __future__ import annotations
from typing import Annotated, List, Optional, Literal, Dict, Any, Tuple
from fastapi import Depends
from datetime import datetime, timezone, date
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

from app.features.journal.models import Journal
from app.features.journal.repository import JournalRepository
from app.features.selfaware.models import Question, Answer, ValueMap, ValueScore
from app.features.selfaware.prompt import emotion_prompt, question_prompt, single_category_prompt, multi_category_prompt, value_score_prompt, value_map_prompt, value_map_short_prompt
from app.features.selfaware.repository import QuestionRepository, AnswerRepository, ValueMapRepository, ValueScoreRepository
from app.features.selfaware.value_map import analyze_personality

class QuestionService:
    def __init__(
        self,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ) -> None:
        self.journal_repository = journal_repository
        self.question_repository = question_repository


    def generate_selfaware_question(self, user_id: int) -> Question:
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
        question = self.question_repository.create_question(
            user_id=user_id,
            question_type="personalized_category",
            text=response,
        )

        return question
    

    def generate_single_category_question(self, user_id: int) -> Question:
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
        question = self.question_repository.create_question(
            user_id=user_id,
            question_type="single_category",
            text=response,
        )

        return question


    def generate_multi_category_question(self, user_id: int) -> Question:
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
        question = self.question_repository.create_question(
            user_id=user_id,
            question_type="multi_category",
            text=response,
        )

        return question

    def generate_question(self, user_id: int) -> Question:
        type = random.randint(0, 2)
        if type == 0:
            return self.generate_selfaware_question(user_id)
        if type == 1:
            return self.generate_single_category_question(user_id)
        if type == 2:
            return self.generate_multi_category_question(user_id)


    def get_questions_by_id(self, question_id: int) -> Question | None:
        return self.question_repository.get_question_by_id(question_id)

    def list_questions_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Question]:
        return self.question_repository.list_questions_by_user(user_id, limit, cursor)   

    def get_questions_by_date(self, user_id: int, date: date) -> Question | None:
        return self.question_repository.get_question_by_date(user_id, date)


class AnswerService:
    def __init__(
        self,
        answer_repository: AnswerRepository,
    ) -> None:
        self.answer_repository = answer_repository


    def create_answer(self, user_id: int, question_id: int, text: str) -> Answer:
        """
        해당 question이 실제 존재하는지 확인한 뒤,
        answer를 DB에 생성한다.
        """
        answer = self.answer_repository.create_answer(
            user_id=user_id,
            question_id=question_id,
            text=text
        )
        return answer


    def get_answer_by_question(self, question_id: int) -> Answer | None:
        return self.answer_repository.get_by_question(question_id)
    
    def list_answers_by_user(self, user_id: int, question_ids: List[int]) -> list[Answer]:
        return self.answer_repository.list_answers_by_user(user_id, question_ids)

    def extract_keyword(self, id: int):
        pass


class ValueScoreService:
    def __init__(
        self,
        question_repository: QuestionRepository,
        answer_repository: AnswerRepository,
        value_score_repository: ValueScoreRepository,
        value_map_repository: ValueMapRepository,
    ) -> None:
        self.question_repository = question_repository
        self.answer_repository = answer_repository
        self.value_score_repository = value_score_repository
        self.value_map_repository = value_map_repository


    def extract_value_score_from_answer(self, user_id:int, question_id:int, answer_id: int):
        llm = ChatOpenAI(model="gpt-5-nano")

        question = self.question_repository.get_question_by_id(question_id)
        answer = self.answer_repository.get_answer_by_id(answer_id)
        value_score_chain = value_score_prompt | llm
        response = value_score_chain.invoke({
            "question": question.text, 
            "answer": answer.text
        })
        content = response.content if hasattr(response, "content") else str(response)

        try:
            data = json.loads(content) # type: ignore
            detected_values = data.get("detected_values", [])

            # 혹은 value_map을 user가 등록되었을 때, craete해도 좋을 듯 합니다
            value_map = self.value_map_repository.get_by_user(user_id)
            if not value_map:
                self.value_map_repository.create_value_map(user_id=user_id)

            for v in detected_values:
                value_score = self.value_score_repository.create_value_score(
                    user_id = user_id,
                    question_id = question_id,
                    answer_id = answer_id,
                    category = v['category_key'],
                    value = v['value_name'],
                    confidence= v['confidence'],
                    intensity= v['intensity'],
                    polarity= v['polarity'],
                    evidence_quotes= v.get('evidence', []),
                )
                self.value_map_repository.update_by_value_score(value_score)
        except json.JSONDecodeError:
            print("JSON 파싱 실패. 모델의 출력 형식을 확인하세요.")
            detected_values = []
        
        return detected_values

    
    def get_top_value_scores(self, user_id: int):
        top_value_scores = self.value_score_repository.get_top_5_value_scores(user_id)
        value_scores = []
        if top_value_scores:
            for top_value_score in top_value_scores:
                value_scores.append({"value": top_value_score.value, "intensity": top_value_score.intensity})
        return value_scores
    
    
class ValueMapService:
    def __init__(
        self,
        value_map_repository: ValueMapRepository,
        value_score_repository: ValueScoreRepository,
    ) -> None:
        self.value_score_repository = value_score_repository
        self.value_map_repository = value_map_repository

    def create_value_map(self, user_id: int) -> None:
        return self.value_map_repository.create_value_map(user_id=user_id)

    def get_value_map_by_user(self, user_id) -> ValueMap | None:
        return self.value_map_repository.get_by_user(user_id)

    def generate_comment(self, user_id: int) -> None:
        value_map = self.value_map_repository.get_by_user(user_id)

        llm = ChatOpenAI(model="gpt-5-nano")
        value_map_chain = value_map_prompt | llm | StrOutputParser()
        value_map_short_chain = value_map_short_prompt | llm | StrOutputParser()

        value_map_text = value_map_chain.invoke(
            {"score_0": value_map.score_0, # type: ignore
             "score_1": value_map.score_1, # type: ignore
             "score_2": value_map.score_2, # type: ignore
             "score_3": value_map.score_3, # type: ignore
             "score_4": value_map.score_4, # type: ignore
             "score_5": value_map.score_5, # type: ignore
             "score_6": value_map.score_6,}) # type: ignore
        
        value_map_short_text = value_map_short_chain.invoke(
            {"score_0": value_map.score_0, # type: ignore
             "score_1": value_map.score_1, # type: ignore
             "score_2": value_map.score_2, # type: ignore
             "score_3": value_map.score_3, # type: ignore
             "score_4": value_map.score_4, # type: ignore
             "score_5": value_map.score_5, # type: ignore
             "score_6": value_map.score_6,}) # type: ignore

        return self.value_map_repository.generate_comment(user_id, value_map_text, value_map_short_text)
