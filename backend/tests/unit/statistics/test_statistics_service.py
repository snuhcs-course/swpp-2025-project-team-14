from collections import namedtuple
from datetime import date
from unittest.mock import Mock

import pytest

from app.features.statistics.service import StatisticsService

# Repository 결과 모킹을 위한 간단한 namedtuple
MockRow = namedtuple("MockRow", ["emotion", "count"])


@pytest.fixture
def mock_stats_repo():
    return Mock()


@pytest.fixture
def stats_service(mock_stats_repo):
    return StatisticsService(statistics_repository=mock_stats_repo)


def test_get_emotion_rates_logic(stats_service, mock_stats_repo):
    # Arrange
    user_id = 1
    start = date(2025, 1, 1)
    end = date(2025, 1, 31)

    # Repository가 반환할 Mock 데이터: Happy 3개, Sad 1개 (총 4개)
    mock_rows = [
        MockRow(emotion="happy", count=3),
        MockRow(emotion="sad", count=1),
    ]
    total_count = 4
    mock_stats_repo.get_emotion_counts.return_value = (mock_rows, total_count)

    # Act
    response = stats_service.get_emotion_rates(user_id, start, end)

    # Assert
    assert response["total_count"] == 4
    stats = response["statistics"]

    # 순서는 보장되지 않으므로 찾아서 검증
    happy_stat = next(s for s in stats if s["emotion"] == "happy")
    sad_stat = next(s for s in stats if s["emotion"] == "sad")

    # Happy: 3/4 = 75.0%
    assert happy_stat["count"] == 3
    assert happy_stat["percentage"] == 75.0

    # Sad: 1/4 = 25.0%
    assert sad_stat["count"] == 1
    assert sad_stat["percentage"] == 25.0


def test_get_emotion_rates_empty(stats_service, mock_stats_repo):
    # Arrange
    mock_stats_repo.get_emotion_counts.return_value = ([], 0)

    # Act
    response = stats_service.get_emotion_rates(1, date.today(), date.today())

    # Assert
    assert response["total_count"] == 0
    assert response["statistics"] == []
