# app/features/selfaware/di.py
from fastapi import Depends
from sqlalchemy.orm import Session

from app.database.session import get_db_session
from app.features.journal.repository import JournalRepository
from app.features.selfaware.repository import (
    QuestionRepository, AnswerRepository,
    ValueMapRepository, ValueScoreRepository,
)
from app.features.selfaware.service import (
    QuestionService, AnswerService, ValueMapService, ValueScoreService,
)
from app.features.analysis.repository import AnalysisRepository
from app.features.analysis.service import AnalysisService

def get_analysis_service(db: Session = Depends(get_db_session)) -> AnalysisService:
    return AnalysisService(
        answer_repository=AnswerRepository(db),
        analysis_repository=AnalysisRepository(db),
    )