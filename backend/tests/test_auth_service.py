from datetime import date
from unittest.mock import Mock, patch

import pytest
from fastapi import HTTPException

from app.features.auth.service import AuthService
from app.features.user.models import User


# Mocking security functions globally for the module
@pytest.fixture
def mock_security():
    with (
        patch("app.features.auth.service.hash_password") as mock_hash,
        patch("app.features.auth.service.verify_password") as mock_verify_pass,
        patch("app.features.auth.service.create_token") as mock_create_token,
        patch("app.features.auth.service.verify_token") as mock_verify_token,
        patch("app.features.auth.service.hash_token") as mock_hash_token,
    ):
        mock_hash.return_value = "hashed_secret"
        mock_verify_pass.return_value = True
        mock_create_token.side_effect = (
            lambda sub, token_type: f"{token_type}_token_for_{sub}"
        )
        mock_hash_token.return_value = "hashed_token_string"

        yield {
            "hash_password": mock_hash,
            "verify_password": mock_verify_pass,
            "create_token": mock_create_token,
            "verify_token": mock_verify_token,
            "hash_token": mock_hash_token,
        }


@pytest.fixture
def auth_service(mock_security):
    user_repo = Mock()
    blocked_token_repo = Mock()
    return AuthService(
        user_repository=user_repo, blocked_token_repository=blocked_token_repo
    )


def test_signup_success(auth_service, mock_security):
    # Arrange
    auth_service.user_repository.get_user_by_login_id.return_value = None
    new_user = User(login_id="new_user", id=1)
    auth_service.user_repository.add_user.return_value = new_user

    # Act
    access, refresh = auth_service.signup(
        login_id="new_user",
        password="password",
        username="User",
        gender="M",
        birthdate=date(2000, 1, 1),
    )

    # Assert
    auth_service.user_repository.add_user.assert_called_once()
    assert access == "access_token_for_new_user"
    assert refresh == "refresh_token_for_new_user"


def test_signup_duplicate_login_id(auth_service):
    # Arrange
    auth_service.user_repository.get_user_by_login_id.return_value = User(id=1)

    # Act & Assert
    with pytest.raises(HTTPException) as exc:
        auth_service.signup("existing", "pw", "u", "M", date(2000, 1, 1))
    assert exc.value.status_code == 400
    assert exc.value.detail == "Same login ID already exists"


def test_login_success(auth_service, mock_security):
    # Arrange
    user = User(login_id="test_user", hashed_password="hashed_secret")
    auth_service.user_repository.get_user_by_login_id.return_value = user
    mock_security["verify_password"].return_value = True

    # Act
    access, refresh = auth_service.login("test_user", "password")

    # Assert
    assert access == "access_token_for_test_user"
    assert refresh == "refresh_token_for_test_user"


def test_login_failure_wrong_password(auth_service, mock_security):
    # Arrange
    user = User(login_id="test_user", hashed_password="hashed_secret")
    auth_service.user_repository.get_user_by_login_id.return_value = user
    mock_security["verify_password"].return_value = False  # 패스워드 불일치

    # Act & Assert
    with pytest.raises(HTTPException) as exc:
        auth_service.login("test_user", "wrong_pw")
    assert exc.value.status_code == 401


def test_validate_refresh_token_blocked(auth_service, mock_security):
    # Arrange
    token = "blocked_token"
    mock_security["verify_token"].return_value = {"sub": "user", "type": "refresh"}
    auth_service.blocked_token_repository.is_token_blocked.return_value = True

    # Act & Assert
    with pytest.raises(HTTPException) as exc:
        auth_service.validate_refresh_token(token)
    assert exc.value.status_code == 401
    assert exc.value.detail == "Token has been revoked"


def test_refresh_success(auth_service, mock_security):
    # Arrange
    old_refresh = "old_refresh_token"
    exp_timestamp = 1700000000
    mock_security["verify_token"].return_value = {
        "sub": "user1",
        "type": "refresh",
        "exp": exp_timestamp,
    }
    auth_service.blocked_token_repository.is_token_blocked.return_value = False

    # Act
    new_access, new_refresh = auth_service.refresh(old_refresh)

    # Assert
    # 1. 새로운 토큰 쌍 발급 확인
    assert new_access == "access_token_for_user1"
    assert new_refresh == "refresh_token_for_user1"

    # 2. 이전 리프레시 토큰 블록 처리 확인
    auth_service.blocked_token_repository.add_blocked_token.assert_called_once()
    # 호출 인자 검증 (token_id와 expired_at)
    args, _ = auth_service.blocked_token_repository.add_blocked_token.call_args
    assert args[0] == "hashed_token_string"  # mocked hash_token result
