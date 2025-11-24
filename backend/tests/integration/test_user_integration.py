from datetime import date

from passlib.context import CryptContext
from sqlalchemy.orm import Session
from starlette.testclient import TestClient

from app.features.user.models import User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# --- 1. 회원가입 (POST /auth/signup) ---


def test_signup_success(client: TestClient, db_session: Session):
    """Test successful user signup with database integration."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user",
        "password": "qwerQWER123!",
        "username": "new-user",
        "gender": "Female",
        "birthdate": date(2002, 2, 2).isoformat(),
    }

    # Act: API 엔드포인트를 호출합니다.
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert 1: API 응답을 검증합니다.
    assert response.status_code == 201
    response_json = response.json()
    assert "access" in response_json
    assert "refresh" in response_json

    # Assert 2: 데이터베이스 상태를 직접 검증합니다.
    user_in_db = (
        db_session.query(User).filter(User.login_id == data["login_id"]).first()
    )

    assert user_in_db is not None
    assert user_in_db.login_id == data["login_id"]
    assert user_in_db.username == data["username"]
    assert user_in_db.hashed_password != data["password"]
    assert user_in_db.gender == data["gender"]
    assert user_in_db.birthdate.isoformat() == data["birthdate"]


def test_signup_duplicate_login_id(
    client: TestClient, db_session: Session, test_user: User
):
    """Test user signup with a duplicate login_id."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다. (test_user)

    # Act: 동일한 login_id로 회원가입을 시도합니다.
    data = {
        "login_id": "test_user",  # 중복된 login_id
        "password": "ValidPass123!",
        "username": "Test-User",
        "gender": "Female",
        "birthdate": date(2002, 2, 2).isoformat(),
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 400
    response_json = response.json()
    assert response_json["detail"] == "Same login ID already exists"


def test_signup_invalid_password(client: TestClient, db_session: Session):
    """Test user signup with an invalid password."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user2",
        "password": "short",  # 너무 짧은 비밀번호
        "username": "new-user2",
        "gender": "Female",
        "birthdate": date(2002, 2, 2).isoformat(),
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 400
    response_json = response.json()
    assert response_json["detail"] == "Invalid format for field password"


def test_signup_invalid_username(client: TestClient, db_session: Session):
    """Test user signup with an invalid username."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user3",
        "password": "ValidPass123!",
        "username": "Invalid Username!",  # 공백과 특수문자가 포함된 username
        "gender": "Female",
        "birthdate": date(2002, 2, 2).isoformat(),
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 400
    response_json = response.json()
    assert response_json["detail"] == "Invalid format for field username"


def test_signup_missing_fields(client: TestClient, db_session: Session):
    """Test user signup with missing required fields."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user4",
        "password": "ValidPass123!",
        "gender": "Female",
        "birthdate": date(2002, 2, 2).isoformat(),
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 422
    response_json = response.json()
    error_detail = response_json["detail"][0]
    assert error_detail["loc"] == ["body", "username"]
    assert error_detail["msg"] == "Field required"
    assert error_detail["type"] == "missing"


# --- 2. 로그인 (POST /auth/login) ---


def test_login_success(client: TestClient, db_session: Session, test_user: User):
    """Test successful user login with database integration."""

    # Act: 올바른 자격 증명으로 로그인 시도
    data = {
        "login_id": "test_user",
        "password": "ValidPass123!",
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 201
    response_json = response.json()
    assert "access" in response_json
    assert "refresh" in response_json


def test_login_invalid_password(
    client: TestClient, db_session: Session, test_user: User
):
    """Test user login with an invalid password."""

    # Act: 잘못된 비밀번호로 로그인 시도
    data = {
        "login_id": "login_user2",
        "password": "WrongPass123!",
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid login ID or password"


def test_login_nonexistent_user(client: TestClient, db_session: Session):
    """Test user login with a non-existent user."""
    # Act: 존재하지 않는 사용자로 로그인 시도
    data = {
        "login_id": "nonexistent_user",
        "password": "SomePass123!",
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid login ID or password"


def test_login_missing_fields(client: TestClient, db_session: Session):
    """Test user login with missing required fields."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "some_user",
        # "password" 필드가 누락되었습니다.
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 422
    response_json = response.json()
    error_detail = response_json["detail"][0]
    assert error_detail["loc"] == ["body", "password"]
    assert error_detail["msg"] == "Field required"
    assert error_detail["type"] == "missing"


# --- 3. 로그아웃 (POST /auth/logout) ---


def test_logout_success(client: TestClient, db_session: Session, test_user: User):
    """Test successful user logout with database integration."""

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "test_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    refresh_token = login_response_json["refresh"]

    # Act: 올바른 리프레시 토큰으로 로그아웃 시도
    logout_data = {
        "refresh": refresh_token,
    }
    response = client.post("/api/v1/auth/logout", json=logout_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["ok"] is True
    assert response_json["data"]["status"] == "success"
    assert response_json["error"] is None


def test_logout_invalid_token(client: TestClient, db_session: Session):
    """Test user logout with an invalid refresh token."""
    # Act: 잘못된 리프레시 토큰으로 로그아웃 시도
    logout_data = {
        "refresh": "invalid_token",
    }
    response = client.post("/api/v1/auth/logout", json=logout_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid token"


# --- 4. 토큰 리프레시 (POST /auth/refresh) ---


def test_refresh_success(client: TestClient, db_session: Session, test_user: User):
    """Test successful token refresh with database integration."""

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "test_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    refresh_token = login_response_json["refresh"]

    # Act: 올바른 리프레시 토큰으로 토큰 갱신 시도
    refresh_data = {
        "refresh": refresh_token,
    }
    response = client.post("/api/v1/auth/refresh", json=refresh_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert "access" in response_json
    assert "refresh" in response_json


def test_refresh_invalid_token(client: TestClient, db_session: Session):
    """Test token refresh with an invalid refresh token."""
    # Act: 잘못된 리프레시 토큰으로 토큰 갱신 시도
    refresh_data = {
        "refresh": "invalid_token",
    }
    response = client.post("/api/v1/auth/refresh", json=refresh_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid token"


# --- 5. 토큰 검증 (POST /auth/verify) ---


def test_verify_success(client: TestClient, db_session: Session, test_user: User):
    """Test successful access token verification with database integration."""

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "test_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    access_token = login_response_json["access"]

    headers = {"Authorization": f"Bearer {access_token}"}
    response = client.post(
        "/api/v1/auth/verify", json={"access": access_token}, headers=headers
    )
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["ok"] is True
    assert response_json["data"]["login_id"] == "test_user"
    assert response_json["error"] is None


def test_verify_invalid_token(client: TestClient, db_session: Session):
    """Test access token verification with an invalid token."""
    # Act: 잘못된 액세스 토큰으로 검증 시도
    verify_data = {
        "access": "invalid_token",
    }
    response = client.post("/api/v1/auth/verify", json=verify_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 403
    response_json = response.json()
    assert response_json["detail"] == "Not authenticated"


# --- 6. 내정보조회 (GET /user/me) ---


def test_me_success(client: TestClient, db_session: Session, test_user: User):
    """Test successful retrieval of user profile with database integration."""

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "test_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    access_token = login_response_json["access"]

    headers = {"Authorization": f"Bearer {access_token}"}
    response = client.get("/api/v1/user/me", headers=headers)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["login_id"] == "test_user"
    assert response_json["username"] == "Test-User"


def test_me_unauthenticated(client: TestClient, db_session: Session):
    """Test retrieval of user profile without authentication."""
    # Act: 인증 없이 사용자 프로필 조회 시도
    response = client.get("/api/v1/user/me")
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 403
    response_json = response.json()
    assert response_json["detail"] == "Not authenticated"


def test_me_invalid_token(client: TestClient, db_session: Session):
    """Test retrieval of user profile with an invalid token."""
    # Act: 잘못된 액세스 토큰으로 사용자 프로필 조회 시도
    headers = {"Authorization": "Bearer invalid_token"}
    response = client.get("/api/v1/user/me", headers=headers)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid token"


# --- 7. 내정보 업데이트 (PATCH /user/me) ---


def test_update_me_success(client: TestClient, db_session: Session, test_user: User):
    """Test successful user profile update."""
    # Arrange: 로그인하여 토큰 발급
    login_data = {"login_id": "test_user", "password": "ValidPass123!"}
    login_resp = client.post("/api/v1/auth/login", json=login_data)
    token = login_resp.json()["access"]
    headers = {"Authorization": f"Bearer {token}"}

    # Act: 회원 정보 수정 요청 (username, gender, appearance, birthdate)
    new_birthdate = date(2000, 1, 1)
    update_data = {
        "username": "Updated-User",
        "gender": "Male",
        "appearance": "Cool Style",
        "birthdate": new_birthdate.isoformat(),
    }

    response = client.patch("/api/v1/user/me", json=update_data, headers=headers)
    db_session.flush()
    db_session.refresh(test_user)

    # Assert: 응답 및 DB 업데이트 검증
    assert response.status_code == 200
    assert response.json() == "Update Success"

    assert test_user.username == update_data["username"]
    assert test_user.gender == update_data["gender"]
    assert test_user.appearance == update_data["appearance"]
    assert test_user.birthdate == new_birthdate


def test_update_me_password(client: TestClient, db_session: Session, test_user: User):
    """Test updating user password and verifying login with new password."""
    # Arrange: 로그인
    login_data = {"login_id": "test_user", "password": "ValidPass123!"}
    token = client.post("/api/v1/auth/login", json=login_data).json()["access"]
    headers = {"Authorization": f"Bearer {token}"}

    # Act: 비밀번호 변경 요청
    new_password = "NewPassword123!"
    update_data = {"password": new_password}

    response = client.patch("/api/v1/user/me", json=update_data, headers=headers)
    db_session.flush()

    # Assert: 변경 성공 확인
    assert response.status_code == 200

    # Act & Assert: 새 비밀번호로 로그인 시도 성공 확인
    new_login_data = {"login_id": "test_user", "password": new_password}
    new_login_resp = client.post("/api/v1/auth/login", json=new_login_data)
    assert new_login_resp.status_code == 201

    # Act & Assert: 구 비밀번호로 로그인 시도 실패 확인
    old_login_data = {"login_id": "test_user", "password": "ValidPass123!"}
    old_login_resp = client.post("/api/v1/auth/login", json=old_login_data)
    assert old_login_resp.status_code == 401


def test_update_me_invalid_data(
    client: TestClient, db_session: Session, test_user: User
):
    """Test user update with invalid data format."""
    # Arrange: 로그인
    login_data = {"login_id": "test_user", "password": "ValidPass123!"}
    token = client.post("/api/v1/auth/login", json=login_data).json()["access"]
    headers = {"Authorization": f"Bearer {token}"}

    # Act: 유효하지 않은 비밀번호 형식으로 업데이트 시도
    update_data = {"password": "short"}
    response = client.patch("/api/v1/user/me", json=update_data, headers=headers)

    # Assert: 400 Bad Request
    assert response.status_code == 400
    # 에러 메시지에 password 관련 내용이 있는지 확인
    assert "password" in str(response.json())


def test_update_me_no_fields(client: TestClient, db_session: Session, test_user: User):
    """Test user update without providing any fields."""
    # Arrange: 로그인
    login_data = {"login_id": "test_user", "password": "ValidPass123!"}
    token = client.post("/api/v1/auth/login", json=login_data).json()["access"]
    headers = {"Authorization": f"Bearer {token}"}

    # Act: 빈 JSON body 전송
    response = client.patch("/api/v1/user/me", json={}, headers=headers)

    # Assert: 400 Bad Request
    assert response.status_code == 400
