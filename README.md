# SWPP Team 14 - Iteraion 1 Working Demo

This repository contains the working demo of iteraion 1.  
The demo demonstrates the backend API implementation of user authentication and session management using **JWT tokens**, tested in a local Docker-based environment.

---

## 🚀 How to Run the Demo

### 1. Prerequisites
Make sure the following tools are installed on your system:
- **Docker** (≥ 24.0)
- **Docker Compose** (≥ 2.0)
- **Python 3.10+** (if you want to test the API manually without Docker)

### 2. Setup & Execution

#### Option 1. Run via Docker Compose (Recommended)
```bash
# Clone this repository
git clone https://github.com/snu-swpp/swpp-2025-project-team-14.git
cd swpp-2025-project-team-14

# Build backend project and start containers
cd backend
docker compose up --build
```
Once the containers are up and running, the backend API will be accessible at:  
`http://localhost:8000`

You can also explore and test all available APIs through the **Swagger UI** at:  
`http://localhost:8000/docs`

---

#### Option 2. Manual Local Run (Without Docker)

If you prefer to run the backend manually without Docker, follow these steps:

```bash
# Clone the repository if you haven't already
git clone https://github.com/snu-swpp/swpp-2025-project-team-14.git
cd swpp-2025-project-team-14/backend

# Create and activate a virtual environment
python3 -m venv venv
source venv/bin/activate   # On Windows use `venv\Scripts\activate`

# Install dependencies
pip install -r requirements.txt

# Set up environment variables as needed (e.g., database URL, secret keys)
# Example:
# export DATABASE_URL=postgresql://user:password@localhost:5432/dbname
# export SECRET_KEY=your_secret_key

# Run database migrations (if applicable)
# alembic upgrade head

# Start the FastAPI server
uvicorn main:app --reload
```
The API will be available at `http://localhost:8000` by default.

---

## 🧩 What the Demo Demonstrates

This demo illustrates the following key features:

- **User Signup:** Register new users with secure password handling.
- **User Login:** Authenticate users and issue JWT access and refresh tokens.
- **Logout Handling:** Invalidate refresh tokens to securely log users out.
- **Token Refresh:** Automatically refresh access tokens using refresh tokens to keep users logged in.
- **Access Token Verification:** Validate and verify access tokens for secure API access.
- **User Information Retrieval:** Fetch authenticated user details using JWT-based access control.

These features form the foundation of a secure and scalable authentication system suitable for modern android applications.

### API Endpoints

- **Signup:** `POST /api/v1/auth/signup`
- **Login:** `POST /api/v1/auth/login`
- **Logout:** `POST /api/v1/auth/logout`
- **Token Refresh:** `POST /api/v1/auth/refresh`
- **Access Token Verification:** `POST /api/v1/auth/verify`
- **Get User Info:** `GET /api/v1/user/me`

---

## 🔧 Example `curl` Requests

You can test the API endpoints using the following example `curl` commands:

- **Signup**
```bash
curl -X POST http://localhost:8000/api/signup/ \
-H "Content-Type: application/json" \
-d '{"login_id": "testuser", "password": "TestPass123", "username": "YourUserName"}'
```

- **Login**
```bash
curl -X POST http://localhost:8000/api/login/ \
-H "Content-Type: application/json" \
-d '{"login_id": "testuser", "password": "TestPass123"}'
```

- **Token Refresh**
```bash
curl -X POST http://localhost:8000/api/token/refresh/ \
-H "Content-Type: application/json" \
-d '{"refresh": "your_refresh_token_here"}'
```

Replace `"your_refresh_token_here"` with the actual refresh token received from the login response.

---

## 🖥️ Environment

This demo is built using the following technologies:

- **FastAPI:** Modern, fast (high-performance) web framework for building APIs with Python 3.10+
- **MySQL:** Robust relational database for storing user data
- **Docker & Docker Compose:** Containerization for consistent development environment
- **JWT Authentication:** Secure, stateless user authentication using JSON Web Tokens

---

## 📜 Notes

This demo serves as a proof-of-concept backend authentication system designed for integration with a android application. It provides a solid foundation for:

- Implementing secure user authentication flows
- Managing user sessions with JWT tokens
- Extending to support roles, permissions, and other features

Next steps include:

- Developing a android application to consume these APIs
- Deploying the system to a production environment
- Implementing other features such as journaling, statistics, analysis, etc.