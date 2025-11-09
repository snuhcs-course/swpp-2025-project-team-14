from datetime import date, datetime
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

security = HTTPBearer()

# ✅ 하나의 analysis 라우터
router = APIRouter(prefix="/analysis", tags=["analysis"])

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
    user: User = Depends(get_current_user)
) -> UserTypeResponse:
    return None

@router.get(
    "/comprehensive-analysis",
    status_code=status.HTTP_201_CREATED,
    summary="Create comprehensive-analysis if not exists. If exists, return the existing one.",
    response_model=ComprehensiveAnalysisResponse,
)
def create_or_get_comprehensive_analysis(
    user: User = Depends(get_current_user)
) -> ComprehensiveAnalysisResponse:
    return None

@router.get(
    "/personalized-advice",
    status_code=status.HTTP_201_CREATED,
    summary="Create a new personalized-advice if not generated today. If not, return the existing one.",
    response_model=PersonalizedAdviceResponse,
)
def create_or_get_personalized_advice(
    user: User = Depends(get_current_user)
) -> PersonalizedAdviceResponse:
    return None

@router.patch(
    "/update",
    status_code=status.HTTP_200_OK,
    summary="Update a analysis (get new neo-pi score, user_type, comprehensive-analysis)",
)
def update_analysis(
    user: User = Depends(get_current_user),
) -> str:
    return "Update Success"