from sqlalchemy.orm import Session
from backend.app.features.question import question_schema as schema
from backend.app.features.question import question_model as model

def create_question(db: Session, question: schema.QuestionCreate):
    db_question = model.Question(**question.dict())
    db.add(db_question)
    db.commit()
    db.refresh(db_question)
    return db_question

def get_user_questions(db: Session, user_id: int):
    return db.query(model.Question).filter(model.Question.user_id == user_id).all()

def get_question(db: Session, question_id: int):
    return db.query(model.Question).filter(model.Question.id == question_id).first()
