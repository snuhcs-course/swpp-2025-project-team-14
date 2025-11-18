from datetime import datetime

from sqlalchemy.orm import Session

from app.features.auth.models import BlockedToken
from app.features.auth.repository import BlockedTokenRepository


def test_add_blocked_token(db_session: Session):
    # Arrange
    repo = BlockedTokenRepository(db_session)
    token_id = "some_hashed_token_id"
    expired_at = datetime(2025, 12, 31, 12, 0, 0)

    # Act
    repo.add_blocked_token(token_id, expired_at)
    db_session.commit()

    # Assert
    in_db = db_session.query(BlockedToken).filter_by(token_id=token_id).first()
    assert in_db is not None
    assert in_db.expired_at == expired_at


def test_is_token_blocked(db_session: Session):
    # Arrange
    repo = BlockedTokenRepository(db_session)
    token_id = "blocked_id"
    expired_at = datetime.now()

    repo.add_blocked_token(token_id, expired_at)
    db_session.commit()

    # Act
    is_blocked = repo.is_token_blocked(token_id)
    is_not_blocked = repo.is_token_blocked("clean_id")

    # Assert
    assert is_blocked is True
    assert is_not_blocked is False
