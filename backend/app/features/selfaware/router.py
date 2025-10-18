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

@question_router.get("/user/{user_id}", 
                     status_code=200,
                     summary="Get a question entry by user(id)",
                     response_model=list[Question])
def get_user_questions(
    user_id: int, 
    question_service: Annotated[QuestionService, Depends()],
):
    """
    특정 사용자의 질문 목록을 조회합니다.
    """
    return question_service.get_questions_by_user(user_id)

@question_router.get("/{question_id}",
                     status_code=200,
                     summary="Get a question entry by ID",
                     response_model=Question)
def get_question(
    question_id: int,
    question_service: Annotated[QuestionService, Depends()],
):
    """
    특정 질문을 조회합니다.
    """
    question = question_service.get_questions_by_id(question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found")
    return question

# Answer

answer_router = APIRouter(prefix="/answers", tags=["answers"])

@answer_router.post("/create",
             status_code=201,
             summary="create answer for the question",
             response_model=Answer)
def create_answer(
    request: AnswerCreateRequest,
    answer_service: Annotated[AnswerService, Depends()],
):
    try:
        return answer_service.create_answer(
            request.text, request.type, request.keywords, request.user_id, request.question_id
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    
@answer_router.get("/question/{question_id}", response_model=list[Answer])
def get_answers_by_question(
    question_id: int,
    answer_service: Annotated[AnswerService, Depends()],
):
    """
    특정 질문(question_id)에 대한 모든 답변 조회
    """
    answers = answer_service.get_answers_by_question(question_id)
    if not answers:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Question(id={question_id})에 대한 답변이 없습니다."
        )
    return answers

@answer_router.get("/user/{user_id}", response_model=list[Answer])
def get_answers_by_user(
    user_id: int,
    answer_service: Annotated[AnswerService, Depends()],
):
    """
    특정 사용자(user_id)가 작성한 답변 조회
    """
    answers = answer_service.get_answers_by_user(user_id)
    if not answers:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User(id={user_id})의 답변이 없습니다."
        )
    return answers

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