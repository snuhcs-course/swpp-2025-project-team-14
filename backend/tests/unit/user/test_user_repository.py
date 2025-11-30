from datetime import date

from sqlalchemy.orm import Session

from app.features.user.models import User
from app.features.user.repository import UserRepository


def test_add_user(db_session: Session):
    # Arrange
    repo = UserRepository(db_session)
    login_id = "new_user"

    # Act
    user = repo.add_user(
        login_id=login_id,
        hashed_password="hashed_secret",
        username="New User",
        gender="Male",
        birthdate=date(2000, 1, 1),
    )
    db_session.commit()

    # Assert
    assert user.id is not None
    assert user.login_id == login_id
    assert user.appearance is None


def test_get_user_by_login_id(db_session: Session):
    # Arrange
    repo = UserRepository(db_session)
    user = User(
        login_id="find_me",
        hashed_password="pwd",
        username="Finder",
        gender="Female",
        birthdate=date(1999, 9, 9),
    )
    db_session.add(user)
    db_session.commit()

    # Act
    found_user = repo.get_user_by_login_id("find_me")
    not_found = repo.get_user_by_login_id("unknown")

    # Assert
    assert found_user is not None
    assert found_user.id == user.id
    assert not_found is None


def test_update_me_repository(db_session: Session):
    # Arrange
    repo = UserRepository(db_session)
    user = User(
        login_id="update_target",
        hashed_password="old_hash",
        username="Old Name",
        gender="Male",
        birthdate=date(1990, 1, 1),
    )
    db_session.add(user)
    db_session.commit()

    # Act
    new_birthdate = date(1995, 5, 5)
    repo.update_me(
        user=user,
        password="NewPassword!",
        username="New Name",
        gender=None,  # 변경 안 함
        birthdate=new_birthdate,
        appearance="New Look",
    )
    db_session.commit()
    db_session.refresh(user)

    # Assert
    assert user.username == "New Name"
    assert user.birthdate == new_birthdate
    assert user.appearance == "New Look"
    assert user.gender == "Male"  # 변경되지 않음
    assert user.hashed_password != "old_hash"
    assert user.hashed_password != "NewPassword!"
