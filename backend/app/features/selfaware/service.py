from dotenv import load_dotenv
load_dotenv()
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from app.database.crud import question_crud
from app.database.schemas import question_schema as schema
from sqlalchemy.orm import Session

def generate_selfaware_question(db: Session, journal_content: str, user_id: int):
    llm = ChatOpenAI(model="gpt-5-nano")
    output_parser = StrOutputParser()
    prompt = ChatPromptTemplate.from_template("""
    사용자의 일기 내용이 주어집니다. 
    이 내용을 바탕으로 사용자가 스스로를 성찰할 수 있도록 돕는 질문을 1개 생성하세요.

    일기 내용:
    {journal}
    """)

    chain = prompt | llm | output_parser
    response = chain.invoke({"journal": journal_content})

    # DB에 저장
    question_data = schema.QuestionCreate(
        user_id=user_id,
        text=response,
        type="selfaware",
    )

    question = question_crud.create_question(db, question_data)

    return question