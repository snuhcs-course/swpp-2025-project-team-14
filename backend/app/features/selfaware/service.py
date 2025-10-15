from dotenv import load_dotenv
load_dotenv()
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from app.database.crud import question_crud
from app.database.schemas import question_schema as schema
from sqlalchemy.orm import Session
from langchain.schema.runnable import RunnableMap
from .prompt import emotion_prompt, question_prompt

def generate_selfaware_question(db: Session, journal_content: str, user_id: int):
    llm = ChatOpenAI(model="gpt-5-nano")
    output_parser = StrOutputParser()

    emotion_chain = emotion_prompt | llm
    question_chain = question_prompt | llm

    reflection_agent = RunnableMap({
        "analysis": emotion_chain,
    }) | question_chain | output_parser

    response = reflection_agent.invoke({"journal": journal_content})

    # DB에 저장
    question_data = schema.QuestionCreate(
        user_id=user_id,
        text=response,
        type="selfaware",
    )

    question = question_crud.create_question(db, question_data)

    return question