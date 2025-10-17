from sqlalchemy.orm import Session
from app.database.models.value_map_model import ValueMap
from app.database.schemas.value_map_schema import ValueMapCreate

def create_value_map(db: Session, value_map_data: ValueMapCreate):
    db_value_map = ValueMap(**value_map_data.dict())
    db.add(db_value_map)
    db.commit()
    db.refresh(db_value_map)
    return db_value_map

def get_latest_value_map(db: Session, user_id: int):
    return (
        db.query(ValueMap)
        .filter(ValueMap.user_id == user_id)
        .order_by(ValueMap.created_at.desc())
        .first()
    )

def get_value_maps_by_user(db: Session, user_id: int):
    return (
        db.query(ValueMap)
        .filter(ValueMap.user_id == user_id)
        .order_by(ValueMap.created_at.desc())
        .all()
    )
