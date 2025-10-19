from fastapi import APIRouter, Depends, HTTPException, status
from typing import Annotated
from sqlalchemy.exc import SQLAlchemyError
from fastapi.security import HTTPBearer
from app.common.schemas import ResponseEnvelope
from .service import QuestionService, AnswerService, ValueMapService
from .schemas.responses import (
    AnswerCreateRequest,
    ValueMap,
    QuestionGenerateRequest,
    ValueMapCreate,
    Answer,
    Question,
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


# -----------------------------
# 💬 Answer 관련 엔드포인트
# -----------------------------
@self_aware_router.post(
    "/answer/create",
    status_code=201,
    summary="Create answer for a question",
    response_model=Answer,
)
def create_answer(
    request: AnswerCreateRequest,
    answer_service: Annotated[AnswerService, Depends()],
):
    try:
        return answer_service.create_answer(
            request.text, request.type, request.keywords, request.user_id, request.question_id
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
# 🗺️ Value Map 관련 엔드포인트
# -----------------------------
@self_aware_router.post(
    "/value-map/generate",
    status_code=201,
    summary="Generate value map based on user's journals",
    response_model=ValueMap,
)
def generate_value_map(
    request: ValueMapCreate,
    value_map_service: Annotated[ValueMapService, Depends()],
):
    """사용자의 일기 기반으로 가치관(Value Map)을 생성합니다."""
    try:
        return value_map_service.analyze_user_personality(request.user_id)
    except SQLAlchemyError as e:
        raise HTTPException(status_code=500, detail=f"데이터베이스 오류 발생: {str(e)}")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
