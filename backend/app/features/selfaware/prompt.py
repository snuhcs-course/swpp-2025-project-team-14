from __future__ import annotations
from typing import Literal
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

value_map_prompt = ChatPromptTemplate.from_template(
    """
    당신은 사람의 가치관과 성향을 분석하여 자연스러운 한국어 문장으로 설명하는 심리 분석 전문가입니다.

    다음은 한 사람의 7가지 가치관 분야별 점수(intensity)입니다:

    - 성장과 자기실현 (Growth & Self-Actualization): {score_0}
    - 관계와 연결 (Relationships & Connection): {score_1}
    - 안정과 안전 (Security & Stability): {score_2}
    - 자유와 자율 (Freedom & Independence): {score_3}
    - 성취와 영향력 (Achievement & Influence): {score_4}
    - 즐거움과 만족 (Enjoyment & Fulfillment): {score_5}
    - 윤리와 초월 (Ethics & Transcendence): {score_6}

    이 정보를 기반으로, 해당 사람이 중요하게 생각하는 가치, 성향, 삶의 우선순위 등을 자연스럽게 3개 내외의 문장으로 요약해 주세요.
    """
)

value_map_short_prompt = ChatPromptTemplate.from_template(
    """
    당신은 사람의 가치관과 성향을 분석하여 자연스러운 한국어 문장으로 설명하는 심리 분석 전문가입니다.

    다음은 한 사람의 7가지 가치관 분야별 점수(intensity)입니다:

    - 성장과 자기실현 (Growth & Self-Actualization): {score_0}
    - 관계와 연결 (Relationships & Connection): {score_1}
    - 안정과 안전 (Security & Stability): {score_2}
    - 자유와 자율 (Freedom & Independence): {score_3}
    - 성취와 영향력 (Achievement & Influence): {score_4}
    - 즐거움과 만족 (Enjoyment & Fulfillment): {score_5}
    - 윤리와 초월 (Ethics & Transcendence): {score_6}

    이 정보를 기반으로, 해당 사람이 중요하게 생각하는 가치, 성향, 삶의 우선순위 등을 자연스럽게 한 문장으로 요약해 주세요.
    """
)