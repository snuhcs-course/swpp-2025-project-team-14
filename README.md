# SWPP Team 14 - Iteraion 2 Working Dmo

This repository contains the working demo of iteraion 2.  
The demo demonstrates the backend API implementation of user authorization and journal features, tested in a local Docker-based environment. AWS EC2 connection and deployment successfully completed. The backend API is now accessible through the EC2 public endpoint(only user authroization feature). In android client, user can try sign up new acount and log in feature.

---

## 🚀 How to Run the Demo in Local

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
`http://localhost:3000`

You can also explore and test all available APIs through the **Swagger UI** at:  
`http://localhost:3000/docs`

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
The API will be available at `http://localhost:3000` by default.


---

## ☁️ AWS EC2 Demo Test (only user authorization feature)

- You can access the FastAPI Swagger UI at:
  ```
  http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/docs
  ```
- If the page loads successfully, the backend is running properly.
- You can only test user authorization feature yet.

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

- **Journal Creation:** Create a new journal entry with text, emotion ratings, and optional image.
- **Journal Retrieval:** Get all journal entries for a specific user or fetch a single entry by ID.
- **Journal Search:** Search journals by title or date.
- **Journal Update:** Edit an existing journal entry.
- **Journal Deletion:** Delete a journal entry by ID.

These features provide create, get, update, delete user's journal entry.

### API Endpoints

- **User Signup:** `POST /api/v1/auth/signup`
- **User Login:** `POST /api/v1/auth/login`
- **User Logout:** `POST /api/v1/auth/logout`
- **Token Refresh:** `POST /api/v1/auth/refresh`
- **Get User Info:** `GET /api/v1/auth/user`

- **Create Journal:** `POST /api/v1/journal/`
- **Get Journals by User:** `GET /api/v1/journal/user/{user_id}`
- **Search Journals:** `GET /api/v1/journal/search`
- **Get Journal by ID:** `GET /api/v1/journal/{journal_id}`
- **Update Journal:** `PATCH /api/v1/journal/{journal_id}`
- **Delete Journal:** `DELETE /api/v1/journal/{journal_id}`

---

## 🔧 Example `curl` Requests

You can test the API endpoints using the following example `curl` commands:

- **Signup**
```bash
curl -X POST http://localhost:3000/api/v1/auth/signup \
-H "Content-Type: application/json" \
-d '{"login_id": "testuser", "password": "TestPass123", "username": "YourUserName"}'
```

- **Login**
```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{"login_id": "testuser", "password": "TestPass123"}'
```

- **Token Refresh**
```bash
curl -X POST http://localhost:3000/api/v1/token/refresh/ \
-H "Content-Type: application/json" \
-d '{"refresh": "your_refresh_token_here"}'
```
Replace `"your_refresh_token_here"` with the actual refresh token received from the login response.

- **Create Journal**
```bash
curl -X POST http://localhost:3000/api/v1/journal/ \
-H "Content-Type: application/json" \
-d '{
  "title": "보람찬 하루",
  "content": "과제가 많지만 열심히하여 많이 끝낼 수 있어서 행복하고 보람있는 하루였다!",
  "emotions": {
    "happy": 4,
    "sad": 0,
    "anxious": 1,
    "calm": 3,
    "annoyed": 1,
    "satisfied": 3,
    "bored": 2,
    "interested": 2,
    "lethargic": 0,
    "energetic": 4
  },
  "gratitude": "맛있는 집밥을 해주신 어머니께 감사하다!"
}'
```
You should fill all 10 types of emotion levels.

---

## 🖥️ Environment

This demo is built using the following technologies:

- **FastAPI:** Modern, fast (high-performance) web framework for building APIs with Python 3.10+
- **MySQL:** Robust relational database for storing user data
- **Docker & Docker Compose:** Containerization for consistent development environment
- **JWT Authentication:** Secure, stateless user authentication using JSON Web Tokens

---

## 📜 Notes

This demo serves as a proof-of-concept backend system designed for integration with an Android application.  
It currently provides a solid foundation for:

- Implementing secure user authentication flows  
- Managing user sessions with JWT tokens  
- Extending to support roles, permissions, and other features  
- AWS EC2 connection for deployment  
- Implemented APIs about **Journal Feature**, enabling creation, retrieval, update, deletion  
- Integrated **User Authentication (Signup, Login, Token Refresh)** features with the Android client application  

Next steps include:

- Complete implementing **Self-Aware Question & Answer** feature APIs 
- Implementing **Statistics** and **Analysis** related backend APIs to support emotional insights and data visualization  
- Further integration of these new APIs with the Android client to provide a complete journaling and self-awareness experience  

---

## 🎥 Demo Video

You can watch the working demo below:

[▶️ Android Signup Demo Videos](https://drive.google.com/file/d/1pNuPoPGSbarzijslBA5ZBo6p0-jGfGu4/view?usp=share_link) \
[▶️ Android Login Demo Videos](https://drive.google.com/file/d/1VRlMx5Eh6b3nfTpIwI3JAgWQ3wGA309u/view?usp=sharing) \
[▶️ Backend Journal Demo Videos](https://drive.google.com/file/d/1mFXd1u9Z6z6V2yC2w5upf6emJ3No3YoU/view?usp=sharing)

> 💡 If the video doesn’t automatically play, right-click the link and choose **“Open in new tab”** or **“Save link as…”** to download it.