from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional, List, Dict, Any

from app.features.selfaware.models import Question, Answer, ValueScore, ValueMap
from app.features.selfaware.prompt import CAT_EN, CAT_KO
from app.features.analysis.models import Analysis

class UserTypeResponse(BaseModel):
    user_type: Optional[str] = None # 분류 유형 ex) 목표 성취형
    updated_at: datetime
    
    @staticmethod
    def from_analysis(analysis: Analysis) -> "UserTypeResponse":
        return UserTypeResponse(
            user_type=analysis.user_type,
            updated_at=analysis.updated_at
        )

class ComprehensiveAnalysisResponse(BaseModel):
    comprehensive_analysis: Optional[str] = None
    updated_at: datetime
    
    @staticmethod
    def from_analysis(analysis: Analysis) -> "ComprehensiveAnalysisResponse":
        return ComprehensiveAnalysisResponse(
            comprehensive_analysis=analysis.comprehensive_analysis,
            updated_at=analysis.updated_at
        )

class PersonalizedAdviceResponse(BaseModel):
    advice_type: Optional[str] = None # 조언 이론 유형
    personalized_advice: Optional[str] = None 
    updated_at: datetime
    
    @staticmethod
    def from_analysis(analysis: Analysis) -> "PersonalizedAdviceResponse":
        return PersonalizedAdviceResponse(
            advice_type = analysis.advice_type,
            personalized_advice=analysis.personalized_advice,
            updated_at=analysis.updated_at
        )