from sqlalchemy.orm import Session
from app.database.schemas import journal_schema as schema
from app.database.models import journal_model as model

def create_journal(db: Session, journal: schema.JournalCreate):
    db_journal = model.Journal(**journal.dict())
    db.add(db_journal)
    db.commit()
    db.refresh(db_journal)
    return db_journal

def get_journal(db: Session, journal_id: int):
    return db.query(model.Journal).filter(model.Journal.id == journal_id).first()

def get_journals_by_user(db: Session, user_id: int):
    return db.query(model.Journal).filter(model.Journal.user_id == user_id).all()

def update_journal(db: Session, journal_id: int, updates: schema.JournalUpdate):
    db_journal = get_journal(db, journal_id)
    if not db_journal:
        return None
    for key, value in updates.dict(exclude_unset=True).items():
        setattr(db_journal, key, value)
    db.commit()
    db.refresh(db_journal)
    return db_journal

def delete_journal(db: Session, journal_id: int):
    db_journal = get_journal(db, journal_id)
    if db_journal:
        db.delete(db_journal)
        db.commit()
    return db_journal
