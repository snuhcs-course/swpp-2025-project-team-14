from datetime import date, datetime, timezone
from typing import Annotated
import asyncio
from sqlalchemy.exc import IntegrityError
from fastapi import APIRouter, Depends, Query, HTTPException, status, BackgroundTasks
from fastapi.security import HTTPBearer

from app.common.authorization import get_current_user
from app.features.user.models import User
from app.features.selfaware.models import Question
from app.features.analysis.models import Analysis

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

from app.features.analysis.schemas.responses import (
    UserTypeResponse,
    ComprehensiveAnalysisResponse,
    PersonalizedAdviceResponse
)
from app.features.analysis.schemas.requests import (
    UserTypeRequest,
    ComprehensiveAnalysisRequest,
    PersonalizedAdviceRequest
)
from app.features.analysis.service import (
    AnalysisService
)
from app.features.analysis.di import get_analysis_service

def utcnow() -> datetime:
    return datetime.now(timezone.utc)

security = HTTPBearer()

# ✅ 하나의 analysis 라우터
router = APIRouter(prefix="/analysis", tags=["analysis"])

# -----------------------------
# 🔧 백그라운드 작업 함수
# -----------------------------
def update_analysis_table(
    user_id: int,
    analysis_service: AnalysisService,
):
    try:
        print("Start updating neo_pi_score")
        analysis_service.update_neo_pi_score(user_id)
        print("Start updating user_id")
        analysis_service.update_user_type(user_id)
        print("Start updating comprehensive_analysis")
        analysis_service.update_comprehensive_analysis(user_id)
        print("Start updating personalized_advice")
        analysis_service.update_personalized_advice(user_id)
    except Exception as e:
        print(f"Error processing updating analysis table for user {user_id}: {e}")
        # 로깅을 위해 에러를 출력하지만 예외를 다시 발생시키지 않음

# -----------------------------
# Analysis 관련 엔드포인트
# -----------------------------

@router.get(
    "/user-type",
    status_code=status.HTTP_201_CREATED,
    summary="Create user_type if not exists. If exists, return the existing one.",
    response_model=UserTypeResponse,
)
def create_or_get_user_type(
    analysis_service: Annotated[AnalysisService, Depends(get_analysis_service)],
    user: User = Depends(get_current_user)
) -> UserTypeResponse:
    analysis = analysis_service.get_analysis_by_user(user_id = user.id)
    if analysis == None:
        raise Exception("User should write selfaware answer first.")
    return UserTypeResponse.from_analysis(analysis)

@router.get(
    "/comprehensive-analysis/{category}",
    status_code=status.HTTP_201_CREATED,
    summary="Create comprehensive-analysis if not exists. If exists, return the existing one.",
    response_model=ComprehensiveAnalysisResponse,
)
def create_or_get_comprehensive_analysis(
    category: str,
    analysis_service: Annotated[AnalysisService, Depends(get_analysis_service)],
    user: User = Depends(get_current_user)
) -> ComprehensiveAnalysisResponse:
    if category not in ['CONSCIENTIOUSNESS', 'NEUROTICISM', 'EXTRAVERSION', 'OPENNESS', 'AGREEABLENESS']:
        raise
    analysis = analysis_service.get_analysis_by_user(user_id = user.id)
    if analysis == None:
        raise Exception("User should write selfaware answer first.")
    return ComprehensiveAnalysisResponse.from_analysis(analysis, category)

@router.get(
    "/personalized-advice",
    status_code=status.HTTP_201_CREATED,
    summary="Create a new personalized-advice if not generated today. If not, return the existing one.",
    response_model=PersonalizedAdviceResponse,
)
def create_or_get_personalized_advice(
    analysis_service: Annotated[AnalysisService, Depends(get_analysis_service)],
    user: User = Depends(get_current_user)
) -> PersonalizedAdviceResponse:
    analysis = analysis_service.get_analysis_by_user(user_id = user.id)
    if analysis == None:
        raise Exception("User should write selfaware answer first.")
    if utcnow().date() != analysis.updated_at.date():
        analysis_service.update_personalized_advice(user_id = user.id)
    return PersonalizedAdviceResponse.from_analysis(analysis)

# 위 api를 없애고, submit_answer background에 추가하는 방안 고려
@router.patch(
    "/update",
    status_code=status.HTTP_200_OK,
    summary="Update a analysis (get new neo-pi score, user_type, comprehensive-analysis)",
)
def update_analysis(
    background_tasks: BackgroundTasks,
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    analysis_service: Annotated[AnalysisService, Depends(get_analysis_service)],
    user: User = Depends(get_current_user),
) -> str:
    answers = answer_service.get_answer_by_user(user_id = user.id)
    if len(answers) < 10:
        raise Exception("You should write more self-analysis QAs")
    if len(answers) % 10 == 0:
        background_tasks.add_task(
            update_analysis_table,
            user.id,
            analysis_service
        )
        return "Update Started"
    return "Update Soon"