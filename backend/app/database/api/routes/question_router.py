# app/routers/question_router.py
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy.exc import SQLAlchemyError
from app.database.session import get_db_session as get_db
from backend.app.features.question.service import generate_selfaware_question
from backend.app.features.question import question_schema as schema
from backend.app.features.question.question_schema import QuestionGenerateRequest

router = APIRouter(prefix="/questions", tags=["questions"])

@router.post("/generate", response_model=schema.Question)
def generate_question(request: QuestionGenerateRequest, db: Session = Depends(get_db)):
    """
    사용자의 일기 내용을 기반으로 LangChain을 이용해 질문을 생성하고 DB에 저장합니다.
    """
    try:
        question = generate_selfaware_question(
            db=db,
            journal_content=request.journal_content,
            user_id=request.user_id
        )
        return question
    except SQLAlchemyError as e:
        raise HTTPException(status_code=500, detail=f"데이터베이스 오류 발생: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"질문 생성 중 오류 발생: {str(e)}")

@router.get("/user/{user_id}", response_model=list[schema.Question])
def get_user_questions(user_id: int, db: Session = Depends(get_db)):
    """
    특정 사용자의 질문 목록을 조회합니다.
    """
    from backend.app.features.question import question_crud
    return question_crud.get_user_questions(db, user_id)

@router.get("/{question_id}", response_model=schema.Question)
def get_question(question_id: int, db: Session = Depends(get_db)):
    """
    특정 질문을 조회합니다.
    """
    from backend.app.features.question import question_crud
    question = question_crud.get_question(db, question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found")
    return question
