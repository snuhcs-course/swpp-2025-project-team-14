from langchain_core.prompts import ChatPromptTemplate

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
