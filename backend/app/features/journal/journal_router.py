from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database.session import get_db_session as get_db
from backend.app.features.journal import journal_schema as schema
from backend.app.features.journal import journal_crud as crud

router = APIRouter(prefix="/journals", tags=["Journal"])

@router.post("/", response_model=schema.JournalRead)
def create_journal(journal: schema.JournalCreate, db: Session = Depends(get_db)):
    return crud.create_journal(db, journal)

@router.get("/{journal_id}", response_model=schema.JournalRead)
def get_journal(journal_id: int, db: Session = Depends(get_db)):
    db_journal = crud.get_journal(db, journal_id)
    if not db_journal:
        raise HTTPException(status_code=404, detail="Journal not found")
    return db_journal

@router.get("/user/{user_id}", response_model=list[schema.JournalRead])
def get_user_journals(user_id: int, db: Session = Depends(get_db)):
    return crud.get_journals_by_user(db, user_id)

@router.put("/{journal_id}", response_model=schema.JournalRead)
def update_journal(journal_id: int, updates: schema.JournalUpdate, db: Session = Depends(get_db)):
    db_journal = crud.update_journal(db, journal_id, updates)
    if not db_journal:
        raise HTTPException(status_code=404, detail="Journal not found")
    return db_journal

@router.delete("/{journal_id}")
def delete_journal(journal_id: int, db: Session = Depends(get_db)):
    crud.delete_journal(db, journal_id)
    return {"message": "Journal deleted successfully"}