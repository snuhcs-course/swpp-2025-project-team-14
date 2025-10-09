import json
from unittest.mock import MagicMock

from fastapi.testclient import TestClient

# Placeholder token data (ensure this matches conftest if you used it)
TEST_ACCESS_TOKEN = "mocked_access_token_123"
TEST_REFRESH_TOKEN = "mocked_refresh_token_456"

# NOTE: The 'client' and 'mock_auth_service' fixtures are assumed to be defined in conftest.py
# and automatically injected by pytest.


##
## 1. /signup
##
def test_signup_success(client: TestClient, mock_auth_service: MagicMock):
    """Test successful user signup."""
    # Arrange
    data = {
        "login_id": "testuser",
        "password": "securepassword",
        "username": "Test User",
    }

    # Act
    response = client.post("/auth/signup", json=data)

    # Assert
    assert response.status_code == 201
    assert response.json() == {
        "ok": True,
        "data": {"access": TEST_ACCESS_TOKEN, "refresh": TEST_REFRESH_TOKEN},
    }
    mock_auth_service.signup.assert_called_once_with(
        data["login_id"], data["password"], data["username"]
    )


##
## 2. /login
##
def test_login_success(client: TestClient, mock_auth_service: MagicMock):
    """Test successful user login."""
    # Arrange
    data = {"login_id": "testuser", "password": "securepassword"}

    # Act
    response = client.post("/auth/login", json=data)

    # Assert
    assert response.status_code == 201
    assert response.json()["data"]["access"] == TEST_ACCESS_TOKEN
    assert response.json()["data"]["refresh"] == TEST_REFRESH_TOKEN
    mock_auth_service.login.assert_called_once_with(data["login_id"], data["password"])


# You should add a test case for login failure (e.g., incorrect credentials)
# which depends on how your auth_service handles it (raising HTTPException).


##
## 3. /logout
##
def test_logout_success(client: TestClient, mock_auth_service: MagicMock):
    """Test successful user logout."""
    # Arrange
    data = {"refresh": TEST_REFRESH_TOKEN}

    # Act
    response = client.post("/auth/logout", json=data)

    # Assert
    assert response.status_code == 200
    assert response.json() == {"ok": True, "data": {"status": "success"}}
    mock_auth_service.validate_refresh_token.assert_called_once_with(TEST_REFRESH_TOKEN)

    # Check if block_refresh_token was called (ignoring the datetime.now() argument)
    assert mock_auth_service.block_refresh_token.called


def test_logout_invalid_token(client: TestClient, mock_auth_service: MagicMock):
    """Test logout with an invalid refresh token."""
    # Arrange
    mock_auth_service.validate_refresh_token.return_value = (
        None  # Simulate validation failure
    )
    data = {"refresh": "invalid_refresh_token"}

    # Act
    response = client.post("/auth/logout", json=data)

    # Assert
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid refresh token"
    mock_auth_service.block_refresh_token.assert_not_called()


def test_logout_missing_token(client: TestClient, mock_auth_service: MagicMock):
    """Test logout with missing refresh token (None case)."""
    # Arrange
    # The RefreshTokenRequest schema likely handles the 'None' case, but we test the logic.
    # We must ensure the request body reflects the schema.
    data = {"refresh": None}

    # Act
    response = client.post("/auth/logout", content=json.dumps(data))

    # Assert
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid refresh token"
    mock_auth_service.validate_refresh_token.assert_not_called()


##
## 4. /refresh
##
def test_refresh_success(client: TestClient, mock_auth_service: MagicMock):
    """Test successful token refresh."""
    # Arrange
    data = {"refresh": TEST_REFRESH_TOKEN}

    # Act
    response = client.post("/auth/refresh", json=data)

    # Assert
    assert response.status_code == 200
    assert response.json()["data"]["access"] == TEST_ACCESS_TOKEN
    assert response.json()["data"]["refresh"] == TEST_REFRESH_TOKEN
    mock_auth_service.refresh.assert_called_once_with(TEST_REFRESH_TOKEN)


# You should add a test case for refresh failure (e.g., invalid token leading to HTTPException).


##
## 5. /verify
##
def test_verify_success(client: TestClient, mock_auth_service: MagicMock):
    """Test successful access token verification."""
    # Arrange
    # The token must be passed in the Authorization header as Bearer token
    headers = {"Authorization": f"Bearer {TEST_ACCESS_TOKEN}"}

    # Act
    response = client.post("/auth/verify", headers=headers)

    # Assert
    assert response.status_code == 200
    assert response.json()["data"]["login_id"] == "test_user_id"
    mock_auth_service.validate_access_token.assert_called_once_with(TEST_ACCESS_TOKEN)


def test_verify_no_token(client: TestClient, mock_auth_service: MagicMock):
    """Test verification without providing an Authorization header."""
    # Act
    response = client.post("/auth/verify")

    # Assert
    assert response.status_code == 403  # HTTPBearer typically returns 403 or 401
    mock_auth_service.validate_access_token.assert_not_called()
