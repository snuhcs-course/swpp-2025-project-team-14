# app/api/routes/answer_router.py
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database.session import get_db_session as get_db
from backend.app.features.answer import answer_schema as schema
from backend.app.features.answer import answer_service

router = APIRouter(prefix="/answers", tags=["answers"])

@router.post("/", response_model=schema.Answer)
def create_answer(answer: schema.AnswerCreate, db: Session = Depends(get_db)):
    """
    새로운 Answer 생성
    """
    try:
        return answer_service.create_answer(db, answer)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )

@router.get("/question/{question_id}", response_model=list[schema.Answer])
def get_answers_by_question(question_id: int, db: Session = Depends(get_db)):
    """
    특정 질문(question_id)에 대한 모든 답변 조회
    """
    answers = answer_service.get_answers_by_question(db, question_id)
    if not answers:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Question(id={question_id})에 대한 답변이 없습니다."
        )
    return answers

@router.get("/user/{user_id}", response_model=list[schema.Answer])
def get_answers_by_user(user_id: int, db: Session = Depends(get_db)):
    """
    특정 사용자(user_id)가 작성한 답변 조회
    """
    answers = answer_service.get_answers_by_user(db, user_id)
    if not answers:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User(id={user_id})의 답변이 없습니다."
        )
    return answers
