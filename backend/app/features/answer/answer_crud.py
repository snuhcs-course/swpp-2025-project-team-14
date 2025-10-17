from sqlalchemy.orm import Session
from app.database.schemas import answer_schema as schema
from app.database.models import answer_model as model

def create_answer(db: Session, answer: schema.AnswerCreate):
    db_answer = model.Answer(**answer.dict())
    db.add(db_answer)
    db.commit()
    db.refresh(db_answer)
    return db_answer

def get_answers_by_question(db: Session, question_id: int):
    return db.query(model.Answer).filter(model.Answer.question_id == question_id).all()

def get_answers_by_user(db: Session, user_id: int):
    return db.query(model.Answer).filter(model.Answer.user_id == user_id).all()