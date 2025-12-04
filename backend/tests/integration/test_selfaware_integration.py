from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.features.user.models import User


def test_create_selfaware_question(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    # 1. API 요청
    response = client.get(
        "/api/v1/self-aware/question?date=2025-11-30", headers=auth_headers
    )
    db_session.flush()
    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201


def test_submit_selfaware_answer(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    # 1. API 요청
    response1 = client.get(
        "/api/v1/self-aware/question?date=2025-11-30", headers=auth_headers
    )
    db_session.flush()
    # 2. 상태 코드 검증 (201)
    assert response1.status_code == 201
    response1_data = response1.json()
    question_id = response1_data["question"]["id"]

    answer_data = {
        "question_id": question_id,
        "text": "오늘은 치킨을 먹어서 기분이 좋았다!",
    }
    response2 = client.post(
        "/api/v1/self-aware/answer", headers=auth_headers, json=answer_data
    )
    assert response2.status_code == 201
