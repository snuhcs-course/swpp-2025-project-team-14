from datetime import date, datetime

from app.features.journal.models import Journal, JournalEmotion


def test_get_emotion_rates(client, db_session, test_user, auth_headers):
    # Arrange: 테스트 데이터 생성
    today = date.today()

    # Journal 생성
    journal = Journal(
        user_id=test_user.id,
        title="Integration Test Journal",
        content="Testing API",
        created_at=datetime.combine(today, datetime.min.time()),
    )
    db_session.add(journal)
    db_session.flush()

    # Emotion 생성 (happy)
    emotion = JournalEmotion(journal_id=journal.id, emotion="happy", intensity=5)
    db_session.add(emotion)
    db_session.commit()

    # Act: API 호출 (query param: start_date, end_date)
    params = {"start_date": today.isoformat(), "end_date": today.isoformat()}
    response = client.get(
        "/api/v1/statistics/emotion-rate", headers=auth_headers, params=params
    )

    # Assert
    assert response.status_code == 200
    data = response.json()

    assert "total_count" in data
    assert "statistics" in data

    assert data["total_count"] == 1
    assert len(data["statistics"]) == 1
    assert data["statistics"][0]["emotion"] == "happy"
    assert data["statistics"][0]["percentage"] == 100.0


def test_get_emotion_rates_unauthorized(client):
    params = {"start_date": "2025-01-01", "end_date": "2025-01-31"}
    response = client.get("/api/v1/statistics/emotion-rate", params=params)
    assert response.status_code == 403
