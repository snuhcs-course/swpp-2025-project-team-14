from sqlalchemy.orm import Session
from app.database.crud import question_crud, answer_crud, value_map_crud
from app.database.schemas.value_map_schema import ValueMapCreate
from app.features.selfaware.value_map_analyzer import analyze_personality

def analyze_user_personality(db: Session, user_id: int):
    # 1. 유저의 질문과 답변을 가져오기
    questions = question_crud.get_user_questions(db, user_id=user_id)
    qa_pairs = []

    for q in questions:
        answers = answer_crud.get_answers_by_question(db, q.id)
        for a in answers:
            qa_pairs.append({"question": q.text, "answer": a.text})

    if not qa_pairs:
        return {"error": "No question-answer data found for this user."}

    result = analyze_personality(qa_pairs)

    value_map_data = ValueMapCreate(user_id=user_id, value_map=result)
    saved_map = value_map_crud.create_value_map(db, value_map_data)

    return {"user_id": user_id, "values": saved_map.value_map, "created_at": saved_map.created_at}