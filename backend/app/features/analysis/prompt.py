from __future__ import annotations

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field


class AdviceGenerationResponse(BaseModel):
    theory: str = Field(description="적용 이론")
    advice: str = Field(description="오늘 하루 실천할 수 있는 피드백")


personalized_advice_prompt = ChatPromptTemplate.from_template(
    """
    당신은 성격심리학을 통합적으로 이해하는 심리 피드백 코치입니다.

    [입력 정보]
    - NEO-PI-R 점수 요약: {neo_pi_summary}
    - 적용 이론: {theory}

    [생성 목표]
    오늘 하루 실천할 수 있는 피드백을 제시하세요.
    - 내용은 사용자의 성격특성에 선택된 이론의 원리를 적용해야 합니다.

    [출력 형식]
    - 자연스럽고 일상적인 문체로 작성합니다.
    - 1문장 이내, 심리 유도형으로 마무리합니다.
    - 해요체로 작성합니다.

    [예시]
    - 적용 이론: CBT
    “완벽히 하려는 마음이 들면, 오늘은 ‘충분히 잘했다’는 말로 스스로를 격려해보세요.”

    - 적용 이론: ACT
    “오늘 느끼는 불편함을 판단하지 말고, 호기심으로 관찰해보세요.”

    - 적용 이론: EQ
    “누군가의 하루를 밝히는 인사를 먼저 건네보세요.”

    Response JSON
    - theory: 적용 이론
    - advice: 오늘 하루 실천할 수 있는 피드백
    """
)
