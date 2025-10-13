# app/routers/question_router.py
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database.session import get_db_session as get_db
from app.features.selfaware.service import generate_selfaware_question
from app.database.schemas import question_schema as schema

router = APIRouter(prefix="/questions", tags=["questions"])

@router.post("/generate", response_model=schema.Question)
def generate_question(journal_content: str, user_id: int, db: Session = Depends(get_db)):
    """
    사용자의 일기 내용을 기반으로 LangChain을 이용해 질문을 생성하고 DB에 저장합니다.
    """
    try:
        question = generate_selfaware_question(db, journal_content, user_id)
        return question
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"질문 생성 중 오류 발생: {str(e)}")

@router.get("/user/{user_id}", response_model=list[schema.Question])
def get_user_questions(user_id: int, db: Session = Depends(get_db)):
    """
    특정 사용자의 질문 목록을 조회합니다.
    """
    from app.database.crud import question_crud
    return question_crud.get_user_questions(db, user_id)

@router.get("/{question_id}", response_model=schema.Question)
def get_question(question_id: int, db: Session = Depends(get_db)):
    """
    특정 질문을 조회합니다.
    """
    from app.database.crud import question_crud
    question = question_crud.get_question(db, question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found")
    return question
