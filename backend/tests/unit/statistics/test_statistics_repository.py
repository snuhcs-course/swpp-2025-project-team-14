from datetime import date, datetime, timedelta

from app.features.journal.models import Journal, JournalEmotion
from app.features.statistics.repository import StatisticsRepository
from app.features.user.models import User


def test_get_emotion_counts(db_session, test_user):
    # Arrange
    repo = StatisticsRepository(db_session)

    target_date = date(2025, 11, 15)

    # 1. 범위 내의 저널 (Target Date) - 'happy'
    journal1 = Journal(
        user_id=test_user.id,
        title="Journal 1",
        content="Content 1",
        created_at=datetime.combine(target_date, datetime.min.time())
        + timedelta(hours=10),
    )
    db_session.add(journal1)
    db_session.flush()

    emotion1 = JournalEmotion(journal_id=journal1.id, emotion="happy", intensity=4)
    db_session.add(emotion1)

    # 2. 범위 내의 저널 (Target Date) - 'sad'
    journal2 = Journal(
        user_id=test_user.id,
        title="Journal 2",
        content="Content 2",
        created_at=datetime.combine(target_date, datetime.min.time())
        + timedelta(hours=12),
    )
    db_session.add(journal2)
    db_session.flush()

    emotion2 = JournalEmotion(journal_id=journal2.id, emotion="sad", intensity=3)
    db_session.add(emotion2)

    # 3. 범위 밖의 저널 (어제)
    journal_past = Journal(
        user_id=test_user.id,
        title="Past Journal",
        content="Past",
        created_at=datetime.combine(
            target_date - timedelta(days=1), datetime.min.time()
        ),
    )
    db_session.add(journal_past)
    db_session.flush()

    emotion_past = JournalEmotion(
        journal_id=journal_past.id, emotion="happy", intensity=4
    )
    db_session.add(emotion_past)

    # 4. 다른 사용자의 저널
    other_user = User(
        login_id="other", hashed_password="pw", gender="M", birthdate=date(2000, 1, 1)
    )
    db_session.add(other_user)
    db_session.flush()

    journal_other = Journal(
        user_id=other_user.id,
        title="Other User Journal",
        content="Other",
        created_at=datetime.combine(target_date, datetime.min.time()),
    )
    db_session.add(journal_other)
    db_session.flush()

    emotion_other = JournalEmotion(
        journal_id=journal_other.id, emotion="happy", intensity=5
    )
    db_session.add(emotion_other)

    db_session.commit()

    # Act: 해당 날짜(target_date) 하루 동안의 통계 조회
    results, total_count = repo.get_emotion_counts(
        test_user.id, target_date, target_date
    )

    # Assert
    assert total_count == 7

    # 결과 리스트 검증
    result_dict = {row.emotion: row.count for row in results}
    assert result_dict["happy"] == 4
    assert result_dict["sad"] == 3
    assert "anxious" not in result_dict
