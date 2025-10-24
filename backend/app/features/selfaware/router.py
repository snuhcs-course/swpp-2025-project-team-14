from datetime import date, datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query, HTTPException, status
from fastapi.security import HTTPBearer

from app.common.authorization import get_current_user
from app.features.user.models import User
from app.features.selfaware.service import (
    QuestionService,
    AnswerService,
    ValueMapService,
    ValueScoreService,
)
from app.features.selfaware.di import (
    get_question_service,
    get_answer_service,
    get_value_map_service,
    get_value_score_service,
)
from app.features.selfaware.schemas.responses import (
    QAResponse,
    QuestionResponse,
    AnswerResponse,
    QACursorResponse,
    ValueMapResponse,
    TopValueScoresResponse,
    PersonalityInsightResponse
)
from app.features.selfaware.schemas.requests import (
    AnswerRequest
)

security = HTTPBearer()

# ✅ 하나의 self-aware 라우터
router = APIRouter(prefix="/self-aware", tags=["selfaware"])

# -----------------------------
# 🧠 Question 관련 엔드포인트
# -----------------------------

@router.get(
    "/question/{question_id}",
    status_code=200,
    summary="Get a specific question by ID",
    response_model=QuestionResponse,
)
def get_question(
    question_id: int,
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    user: User = Depends(get_current_user)
) -> QuestionResponse:
    """특정 질문을 조회합니다."""
    question = question_service.get_questions_by_id(question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found.")
    if question.user_id != user.id:
        raise HTTPException(status_code=403, detail="No Authorization.")
    
    return QuestionResponse.from_question(question)


@router.get(
    "/question",
    response_model=QAResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new question if not exists for today. If exists, return the existing one.",
)
def create_or_get_today_question(
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    user: User = Depends(get_current_user),
    date: date = Query(default=date.today(), description="조회할 날짜 (YYYY-MM-DD)"),
) -> QAResponse:
    """
    오늘 날짜의 질문이 이미 존재하면 해당 질문을 반환하고 (답변이 있으면 함께 반환),
    존재하지 않으면 새로운 질문을 생성하여 저장한 후 반환합니다.
    """
    question = question_service.get_questions_by_date(date)
    if not question: 
        question = question_service.generate_question(user.id)
        return QAResponse(question=QuestionResponse.from_question(question))
    else:
        answer = answer_service.get_answer_by_question(question.id)
        if not answer:
            return QAResponse(question=QuestionResponse.from_question(question))
        else:
            return QAResponse(
                question=QuestionResponse.from_question(question),
                answer=AnswerResponse.from_answer(answer)
            )
            
@router.get(
    "/user/{user_id}",
    response_model=QACursorResponse,
    status_code=status.HTTP_200_OK,
    summary="Get all question & answer pairs by user ID with pagination",
)
def get_user_QAs(
    user_id: int,
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    limit: int = Query(default=10, le=50, description="Number of items to retrieve"),
    cursor: int | None = Query(None, description="ID of the last item from the previous page for pagination"),
    user: User = Depends(get_current_user)
) -> QACursorResponse:
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="No Authorization.")

    questions = question_service.list_questions_by_user(user_id, limit, cursor)
    questions_ids = [q.id for q in questions]
    answers = answer_service.list_answers_by_user(user_id, questions_ids)
    return QACursorResponse.from_QAs(questions, answers)

# -----------------------------
# 💬 Answer 관련 엔드포인트
# -----------------------------

@router.post(
    "/answer",
    response_model=AnswerResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit answer for a question",
)
def submit_answer(
    request: AnswerRequest,
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    user: User = Depends(get_current_user)
) -> AnswerResponse:
    """특정 질문(question_id)에 대한 모든 답변 조회"""
    question_id = request.question_id
    question = question_service.get_questions_by_id(question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found.")
    if question.user_id != user.id:
        raise HTTPException(status_code=403, detail="No Authorization.")

    return answer_service.create_answer(user_id=user.id, question_id=question_id,  text=request.text)
    

# -----------------------------
# 🗺️ Value Map/Score 관련 엔드포인트
# -----------------------------

@router.get(
    "/value-map/{user_id}", 
    response_model=ValueMapResponse,
    status_code=status.HTTP_200_OK,
    summary="Get value map by user ID",
)
def get_value_map_by_user(
    user_id: int,
    value_map_service: Annotated[ValueMapService, Depends(get_value_map_service)],
    user: User = Depends(get_current_user)
) -> ValueMapResponse:
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="No Authorization.")
    value_map = value_map_service.get_value_map_by_user(user_id)
    if not value_map:
        raise HTTPException(status_code=404, detail="Value map not found.")
    return ValueMapResponse.from_value_map(value_map)

    
@router.get(
    "/top-value-scores/{user_id}", 
    response_model=TopValueScoresResponse,
    status_code=status.HTTP_200_OK,
    summary="Get top value scores by user ID",
)
def get_top_value_scores(
    user_id: int,
    value_score_service: Annotated[ValueScoreService, Depends(get_value_score_service)],
    user: User = Depends(get_current_user)
) -> TopValueScoresResponse:
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="No Authorization.")    
    value_scores = value_score_service.get_top_value_scores(user_id)
    return value_scores


@router.get(
    "/{user_id}", 
    response_model=PersonalityInsightResponse,
    status_code=status.HTTP_200_OK,
    summary="Get personality insight by user ID",
)
def get_personality_insight(
    user_id: int,
    value_map_service: Annotated[ValueMapService, Depends(get_value_map_service)],
    user: User = Depends(get_current_user)
) -> PersonalityInsightResponse:
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="No Authorization.")
    value_map = value_map_service.get_value_map_by_user(user_id)
    if not value_map:
        raise HTTPException(status_code=404, detail="Value map not found.")
    return PersonalityInsightResponse.from_value_map(value_map)