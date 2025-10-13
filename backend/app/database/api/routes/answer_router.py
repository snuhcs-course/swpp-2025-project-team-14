from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database.session import get_db_session as get_db
from app.database.schemas import answer_schema as schema
from app.database.crud import answer_crud as crud

router = APIRouter(prefix="/answers", tags=["Answer"])

@router.post("/", response_model=schema.AnswerRead)
def create_answer(answer: schema.AnswerCreate, db: Session = Depends(get_db)):
    return crud.create_answer(db, answer)

@router.get("/question/{question_id}", response_model=list[schema.AnswerRead])
def get_answers_by_question(question_id: int, db: Session = Depends(get_db)):
    return crud.get_answers_by_question(db, question_id)
