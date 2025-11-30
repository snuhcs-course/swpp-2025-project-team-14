from datetime import datetime

import pytest

from app.features.analysis.models import Analysis
from app.features.analysis.repository import AnalysisRepository


@pytest.fixture
def repo(db_session):
    return AnalysisRepository(session=db_session)


# ---------------------------------------
# ✅ 테스트 시작
# ---------------------------------------


def test_create_analysis(repo, db_session):
    analysis = repo.create_analysis(user_id=1)
    db_session.commit()

    result = db_session.query(Analysis).filter_by(user_id=1).first()
    assert result is not None
    assert result.user_id == 1
    assert result.id == analysis.id


def test_get_analysis_by_id(repo, db_session):
    analysis = repo.create_analysis(user_id=2)
    db_session.commit()

    fetched = repo.get_analysis_by_id(analysis.id)
    assert fetched.id == analysis.id
    assert fetched.user_id == 2


def test_get_analysis_by_user_id(repo, db_session):
    repo.create_analysis(user_id=3)
    db_session.commit()

    fetched = repo.get_analysis_by_user_id(3)
    assert fetched is not None
    assert fetched.user_id == 3


def test_update_analysis_user_type(repo, db_session):
    repo.create_analysis(user_id=4)
    db_session.commit()

    repo.update_analysis(user_id=4, user_type="탐험가형")
    updated = repo.get_analysis_by_user_id(4)

    assert updated.user_type == "탐험가형"
    assert isinstance(updated.updated_at, datetime)


def test_update_analysis_multiple_fields(repo, db_session):
    repo.create_analysis(user_id=5)
    db_session.commit()

    neo_pi_score = {"E": 60, "A": 55, "C": 70, "N": 40, "O": 65}
    repo.update_analysis(
        user_id=5,
        neo_pi_score=neo_pi_score,
        conscientiousness="높은 자기통제력",
        personalized_advice="CBT 기반 피드백",
    )
    updated = repo.get_analysis_by_user_id(5)

    assert updated.neo_pi_score == neo_pi_score
    assert updated.conscientiousness == "높은 자기통제력"
    assert updated.personalized_advice == "CBT 기반 피드백"


def test_update_analysis_not_found(repo):
    with pytest.raises(Exception):  # noqa: B017
        repo.update_analysis(user_id=999, user_type="도전형")
