import os
from datetime import date
from unittest.mock import AsyncMock  # AsyncMock 사용

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.features.journal.models import Journal
from app.features.user.models import User

def test_create_selfaware_question(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    # 1. API 요청
    response = client.post("/api/v1/self-aware/question?date=2025-11-27", headers=auth_headers)
    db_session.flush()
    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201