from fastapi import APIRouter, Depends, HTTPException, status, Query
from typing import Annotated
from datetime import date, datetime
from sqlalchemy.exc import SQLAlchemyError
from fastapi.security import HTTPBearer
from app.common.schemas import ResponseEnvelope
from .service import QuestionService, AnswerService, ValueMapService, ValueScoreService
from .schemas.responses import (
    AnswerCreateRequest,
    QuestionGenerateRequest,
    Answer,
    Question,
    QuestionDateResponse,
    AnswerDateResponse,
    QuestionWithAnswerResponse,
    TopValueScoreResponse,
    PersonalityInsightResponse,
    AnswerCreateBody,
    AnswerCreateResponse,
)

security = HTTPBearer()

# ✅ 하나의 self-aware 라우터
self_aware_router = APIRouter(prefix="/self-aware", tags=["Self-Aware"])

# -----------------------------
# 🧠 Question 관련 엔드포인트
# -----------------------------
@self_aware_router.post(
    "/question/generate",
    status_code=201,
    summary="Generate self-aware question",
    response_model=Question,
)
def generate_question(
    request: QuestionGenerateRequest,
    question_service: Annotated[QuestionService, Depends()],
):
    """
    사용자의 일기 내용을 기반으로 LangChain을 이용해 질문을 생성하고 DB에 저장합니다.
    """
    try:
        question = question_service.generate_selfaware_question(request.user_id)
        return question
    except SQLAlchemyError as e:
        raise HTTPException(status_code=500, detail=f"데이터베이스 오류 발생: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"질문 생성 중 오류 발생: {str(e)}")


@self_aware_router.get(
    "/question/user/{user_id}",
    status_code=200,
    summary="Get all questions by user",
    response_model=list[Question],
)
def get_user_questions(
    user_id: int,
    question_service: Annotated[QuestionService, Depends()],
):
    """특정 사용자의 질문 목록을 조회합니다."""
    return question_service.get_questions_by_user(user_id)


@self_aware_router.get(
    "/question/{question_id}",
    status_code=200,
    summary="Get a specific question by ID",
    response_model=Question,
)
def get_question(
    question_id: int,
    question_service: Annotated[QuestionService, Depends()],
):
    """특정 질문을 조회합니다."""
    question = question_service.get_questions_by_id(question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found")
    return question

@self_aware_router.get("/question",
    response_model=QuestionWithAnswerResponse,
)
def get_selfaware_question(
    question_service: Annotated[QuestionService, Depends()],
    answer_service: Annotated[AnswerService, Depends()],
    date: date = Query(..., description="조회할 날짜 (YYYY-MM-DD)"),
):
    """
    특정 날짜의 self-aware question을 반환합니다.
    """
    question = question_service.get_questions_by_date(date)

    if not question:
        raise HTTPException(status_code=404, detail="해당 날짜의 질문이 없습니다.")
    
    question_response = QuestionDateResponse.model_validate(question)

    answer = answer_service.get_answers_by_question(question[id])
    if not answer:
        return QuestionWithAnswerResponse(question=question_response)
    
    answer_response = AnswerDateResponse.model_validate(answer)

    return QuestionWithAnswerResponse(question=question_response, answer=answer_response)

# -----------------------------
# 💬 Answer 관련 엔드포인트
# -----------------------------
@self_aware_router.post(
    "/value-map",
    status_code=201,
    summary="Create answer for a question",
    response_model=AnswerCreateResponse,
)
def create_answer(
    request: AnswerCreateRequest,
    answer_service: Annotated[AnswerService, Depends()],
):
    try:
        return answer_service.create_answer(
            request.text, request.question_id
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))



@self_aware_router.get(
    "/answer/question/{question_id}",
    response_model=list[Answer],
    summary="Get all answers for a specific question",
)
def get_answers_by_question(
    question_id: int,
    answer_service: Annotated[AnswerService, Depends()],
):
    """특정 질문(question_id)에 대한 모든 답변 조회"""
    answers = answer_service.get_answers_by_question(question_id)
    if not answers:
        raise HTTPException(
            status_code=404,
            detail=f"Question(id={question_id})에 대한 답변이 없습니다.",
        )
    return answers


@self_aware_router.get(
    "/answer/user/{user_id}",
    response_model=list[Answer],
    summary="Get all answers by user",
)
def get_answers_by_user(
    user_id: int,
    answer_service: Annotated[AnswerService, Depends()],
):
    """특정 사용자(user_id)가 작성한 답변 조회"""
    answers = answer_service.get_answers_by_user(user_id)
    if not answers:
        raise HTTPException(
            status_code=404,
            detail=f"User(id={user_id})의 답변이 없습니다.",
        )
    return answers


# -----------------------------
# 🗺️ Value Map/Score 관련 엔드포인트
# -----------------------------
@self_aware_router.get("/top-value-scores/{user_id}", response_model=TopValueScoreResponse)
def get_top_value_scores(
    user_id: int,
    value_score_service: Annotated[ValueScoreService, Depends()],
):
    return value_score_service.get_top_value_scores(user_id)

@self_aware_router.get("/{user_id}", response_model=PersonalityInsightResponse)
def get_personality_insight(
    user_id: int,
    value_map_service: Annotated[ValueMapService, Depends()],
):
    value_map = value_map_service.get_value_map_by_user(user_id)
    if not value_map:
        raise
    response = PersonalityInsightResponse(
        user_id = user_id,
        personality_insight = value_map.personality_insight,
        updated_at = datetime.utcnow(),
    )
    return response