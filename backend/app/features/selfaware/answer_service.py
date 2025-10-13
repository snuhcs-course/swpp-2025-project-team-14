from sqlalchemy.orm import Session
from app.database.schemas import answer_schema as schema
from app.database.crud import answer_crud, question_crud

def create_answer(db: Session, answer: schema.AnswerCreate):
    """
    해당 question이 실제 존재하는지 확인한 뒤,
    answer를 DB에 생성한다.
    """
    question = question_crud.get_question(db, answer.question_id)
    if not question:
        raise ValueError(f"Question(id={answer.question_id})이 존재하지 않습니다.")
    
    return answer_crud.create_answer(db, answer)


def get_answers_by_question(db: Session, question_id: int):
    """
    특정 question_id에 속한 모든 답변을 반환
    """
    return answer_crud.get_answers_by_question(db, question_id)


def get_answers_by_user(db: Session, user_id: int):
    """
    특정 user_id가 작성한 모든 답변을 반환
    """
    return answer_crud.get_answers_by_user(db, user_id)

