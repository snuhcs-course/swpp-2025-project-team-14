from unittest.mock import Mock

import pytest

from app.features.user.errors import UserUpdateError
from app.features.user.models import User
from app.features.user.service import UserService


@pytest.fixture
def mock_user_repository():
    return Mock()


@pytest.fixture
def user_service(mock_user_repository):
    return UserService(user_repository=mock_user_repository)


def test_get_user_by_login_id(user_service, mock_user_repository):
    # Arrange
    login_id = "test_user"
    expected_user = User(login_id=login_id)
    mock_user_repository.get_user_by_login_id.return_value = expected_user

    # Act
    result = user_service.get_user_by_login_id(login_id)

    # Assert
    assert result == expected_user
    mock_user_repository.get_user_by_login_id.assert_called_once_with(login_id)


def test_update_me_success(user_service, mock_user_repository):
    # Arrange
    user = User(id=1, username="old_name")
    password = "NewPass123!"
    username = "new_name"

    # Act
    user_service.update_me(
        user=user,
        password=password,
        username=username,
        gender=None,
        birthdate=None,
        appearance=None,
    )

    # Assert
    mock_user_repository.update_me.assert_called_once_with(
        user=user,
        password=password,
        username=username,
        gender=None,
        birthdate=None,
        appearance=None,
    )


def test_update_me_no_fields(user_service, mock_user_repository):
    # Arrange
    user = User(id=1)

    # Act & Assert
    # 아무 필드도 제공되지 않으면 UserUpdateError가 발생해야 함
    with pytest.raises(UserUpdateError):
        user_service.update_me(
            user=user,
            password=None,
            username=None,
            gender=None,
            birthdate=None,
            appearance=None,
        )

    # Repository 메소드는 호출되지 않아야 함
    mock_user_repository.update_me.assert_not_called()
