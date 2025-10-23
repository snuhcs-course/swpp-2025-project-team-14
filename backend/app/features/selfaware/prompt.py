from __future__ import annotations
from typing import List, Optional, Literal, Dict, Any, Tuple
from langchain_core.prompts import ChatPromptTemplate

CATEGORIES = [
    ("Growth & Self-Actualization", "성장과 자기실현"),
    ("Relationships & Connection",  "관계와 연결"),
    ("Security & Stability",        "안정과 안전"),
    ("Freedom & Independence",      "자유와 자율"),
    ("Achievement & Influence",     "성취와 영향력"),
    ("Enjoyment & Fulfillment",     "즐거움과 만족"),
    ("Ethics & Transcendence",      "윤리와 초월"),
]
CAT_EN = [en for en, _ in CATEGORIES]
CAT_KO = {en: ko for en, ko in CATEGORIES}

Language = Literal["ko", "en"]
QuestionType = Literal[
    "single_category",      # 하나의 가치 카테고리에 대한 질문
    "multi_category",       # 여러 가치 카테고리에 대한 복합 질문
    "personalized_category" # 사용자의 일기 데이터를 바탕으로 한 질문
]

# 감정 분석 프롬프트
emotion_prompt = ChatPromptTemplate.from_template(
    """
    다음 일기의 주요 감정과 그 이유를 분석해줘.
    감정은 하나 이상일 수 있고, 가능한 한 구체적으로 설명해줘.

    일기:
    {journal}
    """
)

# 자기 성찰 질문 생성 프롬프트
question_prompt = ChatPromptTemplate.from_template(
    """
    다음 감정 분석 결과를 기반으로,
    사용자가 자기 성찰을 할 수 있도록 돕는 질문 1개를 만들어줘.
    질문은 구체적이고, 감정의 원인을 탐색할 수 있도록 구성해줘.

    감정 분석 결과:
    {analysis}
    """
)

single_category_prompt = ChatPromptTemplate.from_template(
    """You are a coaching-style question designer for a self-reflection diary app.

    Goal: Create exactly ONE open-ended question about a single value category, natural and everyday-language (no category names).

    Target category: {cat}

    Guidelines:
    - Invite a concrete episode, feelings, and why it mattered.
    - Avoid yes/no; ask one sentence only.
    - Tone/Style: "사려 깊고 비판단적"
    - 질문은 한국어 한 문장으로 작성하세요.

    Return JSON:
    - text
    - rationale
    """
)

multi_category_prompt = ChatPromptTemplate.from_template(
    """You are a coaching-style question designer for a self-reflection diary app.

    Goal: Create exactly ONE open-ended question that explores tensions/trade-offs or priorities ACROSS multiple value categories, without naming categories.

    Target categories:
    {cats}

    Guidelines:
    - Encourage reflection on how the user balances these values in real-life decisions.
    - Ask for a specific situation or episode.
    - Avoid yes/no; ask one sentence only.
    - Tone/Style: "사려 깊고 비판단적"
    - 질문은 한국어 한 문장으로 작성하세요.

    Return JSON:
    - text
    - rationale
    """
)

# get value score from prompt
value_score_prompt = ChatPromptTemplate.from_template(
    """You are extracting personal values from a diary answer for a self-reflection app.

    "한국어로 추출하되, 값 이름은 영어 표준명(예: Family, Freedom)으로 반환하세요.

    Rules:
    - Detect up to 6 concrete 'values' (e.g., Family, Freedom, Achievement, Health, Authenticity).
    - For each value, return:
    - value_name (english canonical if possible)
    - category_key (one of: 'Growth & Self-Actualization','Relationships & Connection','Security & Stability','Freedom & Independence','Achievement & Influence','Enjoyment & Fulfillment','Ethics & Transcendence')
    - confidence [0..1]
    - intensity [0..1] (how strongly the value was expressed)
    - polarity in [-1, 0, +1]
    - evidence: short quotes from the answer (1~2)
    - If unsure of category_key, leave it null.

    {question}
    Answer:
    \"\"\"{answer}\"\"\"

    Return JSON with:
    - detected_values: list of objects
    """
)