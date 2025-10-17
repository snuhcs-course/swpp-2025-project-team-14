from dotenv import load_dotenv
load_dotenv()
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from backend.app.features.journal import journal_crud
from backend.app.features.question import question_crud
from backend.app.features.question import question_schema as schema
from sqlalchemy.orm import Session
from langchain.schema.runnable import RunnableMap
from .prompt import emotion_prompt, question_prompt

def generate_selfaware_question(db: Session, user_id: int):
    """
    특정 사용자의 일기를 DB에서 불러와 요약 → 자기성찰 질문을 생성 → DB 저장
    """
    llm = ChatOpenAI(model="gpt-5-nano")
    output_parser = StrOutputParser()

    # ✅ 1. 유저의 일기 가져오기
    journals = journal_crud.get_journals_by_user(db, user_id)
    if not journals or len(journals) == 0:
        raise ValueError(f"User {user_id} has no journal entries.")

    # ✅ 2. 최근 일기 3~5개만 선택 (토큰 제한 방지)
    recent_journals = journals[-5:]
    combined_content = "\n".join([f"- {j.content}" for j in recent_journals])

    # ✅ 3. 일기 요약 (너무 길면 LLM 부담됨)
    summary_prompt = ChatPromptTemplate.from_template(
        "다음은 한 사용자의 최근 일기 내용입니다. 이 일기들의 전반적인 감정과 주제를 간결하게 요약해 주세요.\n\n{journal_text}"
    )
    summary_chain = summary_prompt | llm | StrOutputParser()
    journal_summary = summary_chain.invoke({"journal_text": combined_content})

    # ✅ 4. 감정 분석 → 자기성찰 질문 생성
    emotion_chain = emotion_prompt | llm
    question_chain = question_prompt | llm

    reflection_agent = RunnableMap({
        "analysis": emotion_chain,
    }) | question_chain | output_parser

    response = reflection_agent.invoke({"journal": journal_summary})

    # DB에 저장
    question_data = schema.QuestionCreate(
        user_id=user_id,
        text=response,
        type="selfaware",
    )

    question = question_crud.create_question(db, question_data)

    return question