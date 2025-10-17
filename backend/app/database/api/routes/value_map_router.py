from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from typing import List
from app.database.session import get_db_session as get_db
from backend.app.features.value_map.value_map_schema import ValueMapResponse
from backend.app.features.value_map import value_map_crud

router = APIRouter(prefix="/value-maps", tags=["Value Maps"])

@router.get("/latest/{user_id}", response_model=ValueMapResponse)
def get_latest_value_map(user_id: int, db: Session = Depends(get_db)):
    value_map = value_map_crud.get_latest_value_map(db, user_id)
    if not value_map:
        return {"message": "No value map found for this user."}
    return value_map

@router.get("/user/{user_id}", response_model=List[ValueMapResponse])
def get_user_value_maps(user_id: int, db: Session = Depends(get_db)):
    return value_map_crud.get_value_maps_by_user(db, user_id)