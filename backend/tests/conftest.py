from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from ..app.main import app

# Placeholder token data for consistent testing
TEST_ACCESS_TOKEN = "mocked_access_token_123"
TEST_REFRESH_TOKEN = "mocked_refresh_token_456"


@pytest.fixture
def mock_auth_service(mocker):
    """
    Fixture to mock the AuthService dependency.
    """
    mock = MagicMock()

    # Configure mock return values for key methods
    mock.signup.return_value = (TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN)
    mock.login.return_value = (TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN)
    mock.refresh.return_value = (TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN)
    mock.validate_refresh_token.return_value = True  # Assume valid by default
    mock.validate_access_token.return_value = {"sub": "test_user_id"}

    # Use mocker to replace the dependency in the FastAPI app
    # NOTE: You need to adjust the path below to where your actual AuthService is imported/used
    mocker.patch(
        "app.features.auth.service.AuthService",  # The actual path to AuthService
        return_value=mock,
    )
    return mock


@pytest.fixture
def client(mock_auth_service):
    """
    Fixture for the TestClient, which automatically uses the mocked AuthService.
    """
    return TestClient(app)
