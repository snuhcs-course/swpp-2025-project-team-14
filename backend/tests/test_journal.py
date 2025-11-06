from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.features.journal.models import Journal
from app.features.user.models import User


def test_create_journal_success(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    """
    일지 생성 (POST /api/v1/journal) 성공 테스트
    """
    journal_data = {
        "title": "오늘의 일지",
        "content": "오늘은 테스트 코드를 작성했다.",
        "emotions": {"happy": 1, "sad": 3, "anxious": 1, "calm": 3, "lethargic": 4},
        "gratitude": "오늘 살아있음에 감사하다.",
    }

    # 1. API 요청
    response = client.post("/api/v1/journal", headers=auth_headers, json=journal_data)
    db_session.flush()

    # 2. 상태 코드 검증 (생성 성공 201)
    assert response.status_code == 201

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert response_data["title"] == journal_data["title"]
    assert response_data["content"] == journal_data["content"]
    assert response_data["gratitude"] == journal_data["gratitude"]
    assert "id" in response_data

    db_journal = (
        db_session.query(Journal).filter(Journal.id == response_data["id"]).first()
    )
    assert db_journal is not None
    assert db_journal.title == "오늘의 일지"
    assert db_journal.user_id == test_user.id
