from typing import Annotated

from fastapi import Depends
from sqlalchemy.orm import Session

from app.common.utilities import get_korea_time
from app.database.session import get_db_session
from app.features.analysis.models import Analysis


# -------------------------------
# Analysis Repository
# -------------------------------
class AnalysisRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_analysis(
        self,
        user_id: int,
    ) -> Analysis:
        analysis = Analysis(
            user_id=user_id,
        )
        self.session.add(analysis)
        self.session.flush()
        return analysis

    def get_analysis_by_id(self, id: int) -> Analysis | None:
        return self.session.get(Analysis, id)

    def get_analysis_by_user_id(self, user_id: int) -> Analysis | None:
        return self.session.query(Analysis).filter(Analysis.user_id == user_id).first()

    def update_analysis(
        self,
        user_id: int,
        user_type: str | None = None,
        neo_pi_score: dict | None = None,
        conscientiousness: str | None = None,
        neuroticism: str | None = None,
        extraversion: str | None = None,
        openness: str | None = None,
        agreeableness: str | None = None,
        advice_type: str | None = None,
        personalized_advice: str | None = None,
    ):
        analysis = self.get_analysis_by_user_id(user_id)
        if analysis is None:
            raise

        if user_type is not None:
            analysis.user_type = user_type
        if neo_pi_score is not None:
            analysis.neo_pi_score = neo_pi_score
        if conscientiousness is not None:
            analysis.conscientiousness = conscientiousness
        if neuroticism is not None:
            analysis.neuroticism = neuroticism
        if extraversion is not None:
            analysis.extraversion = extraversion
        if openness is not None:
            analysis.openness = openness
        if agreeableness is not None:
            analysis.agreeableness = agreeableness
        if advice_type is not None:
            analysis.advice_type = advice_type
        if personalized_advice is not None:
            analysis.personalized_advice = personalized_advice

        analysis.updated_at = get_korea_time()

        self.session.flush()
        self.session.commit()  # background에서 진행 예정
