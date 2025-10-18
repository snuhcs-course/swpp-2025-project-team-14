from fastapi import APIRouter, Depends, HTTPException, status
from typing import List
from sqlalchemy.orm import Session
from app.database.session import get_db_session as get_db
from sqlalchemy.exc import SQLAlchemyError
from typing import Annotated
from datetime import datetime
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from app.common.schemas import ResponseEnvelope
from .service import QuestionService, AnswerService, ValueMapService
from .schemas.responses import AnswerCreateRequest, ValueMap, QuestionGenerateRequest, ValueMapCreate, Answer, Question
# Question

question_router = APIRouter(prefix="/questions", tags=["questions"])
security = HTTPBearer()

@question_router.post("/generate",
             status_code=201,
             summary="Generate selfaware question",
             description="Create a selfaware question based on user's journal.",
             response_model=Question)
def generate_question(
    request: QuestionGenerateRequest,
    question_service: Annotated[QuestionService, Depends()],
):
    """
    사용자의 일기 내용을 기반으로 LangChain을 이용해 질문을 생성하고 DB에 저장합니다.
    """
    try:
        question = question_service.generate_selfaware_question(
            request.user_id
        )
        return question
    except SQLAlchemyError as e:
        raise HTTPException(status_code=500, detail=f"데이터베이스 오류 발생: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"질문 생성 중 오류 발생: {str(e)}")

# Answer

answer_router = APIRouter(prefix="/answers", tags=["answers"])

@answer_router.post("/create",
             status_code=201,
             summary="create answer for the question",
             description="Store answer for the selfaware question",
             response_model=Answer)
def create_answer(
    request: AnswerCreateRequest,
    answer_service: Annotated[AnswerService, Depends()],
):
    """
    새로운 Answer 생성
    """
    try:
        return answer_service.create_answer(
            request.text, request.type, request.keywords, request.user_id, request.question_id
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )

# Value Map

value_map_router = APIRouter(prefix="/value-maps", tags=["Value Maps"])

@value_map_router.post("/generate",
             status_code=201,
             summary="Generate selfaware question",
             description="Create a selfaware question based on user's journal.",
             response_model=ValueMap)
def generate_value_map(
    request: ValueMapCreate,
    value_map_service: Annotated[ValueMapService, Depends()],
):
    try:
        return value_map_service.analyze_user_personality(
            request.user_id
        )
    except SQLAlchemyError as e:
        raise HTTPException(status_code=500, detail=f"데이터베이스 오류 발생: {str(e)}")
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )