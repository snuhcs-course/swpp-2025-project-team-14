# SWPP Team 14 - Iteration 5 Working Demo

The demo demonstrates the backend API implementation of user authorization, journal features, self‑aware features, as well as newly added **journal statistics** and **personality analysis** functionalities. All components are tested in a local Docker‑based environment. AWS EC2 connection and deployment were successfully completed, and the backend API is now accessible through the EC2 public endpoint (currently providing user authorization, journal features, self‑aware features, and partial statistics/analysis).  
In the Android client, users can sign up, log in, create journals, view statistics, use self‑aware features, and check persaonlity analysis.

---

## 🚀 How to Run the Demo in Local

### 1. Prerequisites
Make sure the following tools are installed on your system:
- **Docker** (≥ 24.0)
- **Docker Compose** (≥ 2.0)
- **Python 3.10+** (if you want to test the API manually without Docker)

### 2. Setup & Execution 

#### 2.1. Backend

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

##### 🧩 Local Environment Configuration (`.env.local`)

Before running the backend locally, make sure to create a `.env.local` file inside the `backend/` directory.  
This file defines essential environment variables required for the FastAPI application to run correctly.

Example `.env.local`:

```bash
# App
APP_NAME=MindLog-Backend
APP_ENV=local
APP_HOST=0.0.0.0
APP_PORT=3000
APP_DEBUG=true
TIMEZONE=Asia/Seoul

# Security
JWT_SECRET=<random_secret_key>
JWT_ALG=HS256
JWT_ACCESS_TOKEN_EXPIRE_MINUTES=30
JWT_REFRESH_TOKEN_EXPIRE_DAYS=30

# Database
DB_HOST=db
DB_PORT=3306
DB_USER=root
DB_PASSWORD=password
DB_NAME=mindlog_db
DATABASE_URL=mysql+pymysql://root:password@db:3306/mindlog_db

# AWS S3
AWS_ACCESS_KEY_ID=<your_aws_key>
AWS_SECRET_ACCESS_KEY=<your_secret_key>

# OpenAI
OPENAI_API_KEY=<your_openai_key>
```

> ⚠️ **Note:**  
> - The `.env.local` file is for local testing only and **must not be committed to GitHub**.  
> - Docker Compose automatically loads this file during build.  
> - If you modify environment variables, rebuild containers with:
>   ```bash
>   docker compose down
>   docker compose up --build
>   ```

---

#### 2.2. Android

To run the Android client app connected to the backend:

1. **Open Android Studio** and import the project from:
   ```
   swpp-2025-project-team-14/android
   ```
2. Ensure the backend server is already running locally (via Docker or manual FastAPI execution) or deployed on AWS EC2.
   - If running locally: set `API_BASE_URL` in `gradle.properties` or `BuildConfig` to:
     ```
     http://10.0.2.2:3000/
     ```
     (This is the Android emulator’s alias for `localhost`.)
   - If testing with the deployed backend: set the URL to your EC2 endpoint, e.g.:
     ```
     http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/
     ```
3. **Build & Run** the app on an Android emulator or physical device.
   - Recommended SDK: Android 14 (API level 34)
   - Minimum SDK: Android 8.0 (API level 26)

4. **Available features in this demo**:
   - Signup & Login with JWT authentication
   - Token refresh & persistent login
   - Journal creation, retrieval, update, and deletion
   - Journal image upload via AWS S3 presigned URLs
   - AI keyword extraction and reflection analysis
   - Self-Aware Question & Answer integration
   - UI for analysis and statistics features
   - Journal statistics: emotion ratio, emotion trends, events causing specfic emotion, keyword frequency
   - Personality analysis: user type classification, Five‑Factor insights, personalized advice

The app automatically connects to the backend endpoint defined in the environment configuration and synchronizes user data securely.

---

## ☁️ AWS EC2 Demo Test (user authorization, journal features)

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

- **Journal Creation:** Create a new journal entry with text, emotion level, and optional image.
- **Journal Retrieval:** Get all journal entries of user.
- **Journal Search:** Search journals by keyword or date.
- **Journal Update:** Edit an existing journal entry.
- **Journal Deletion:** Delete a journal entry by ID.
- **Journal Image Upload:** Securely upload images to AWS S3 using presigned URLs.
- **Journal Image Generation:** Request AI to generate image related to content of journal.
- **Journal Keyword Extraction:** Extract emotion-related keywords from journals using an OpenAI LLM (via LangChain). 

These features provide create, get, update, delete user's journal entry.

- **Self-Aware Question & Answer:** Interactive Q&A feature to promote self-awareness and reflection.
- **AI Integration:** Use of OpenAI models for value extraction and reflection analysis.

- **Journal Statistics:** Aggregation of emotion ratios, emotion trends over time, emotion-triggering events, and keyword frequencies extracted from journal entries.
- **Comprehensive Analysis:** Personality insights based on Five-Factor theory, including user type classification, descriptive summaries, and personalized daily advice generated from self-aware Q&A data.
- **End-to-End Flow:** Demonstration of how journal data and self-aware answers are combined into statistical summaries and psychological analysis, fully integrated with the Android frontend.

### API Endpoints

#### Auth
- **User Signup:** `POST /api/v1/auth/signup`
- **User Login:** `POST /api/v1/auth/login`
- **User Logout:** `POST /api/v1/auth/logout`
- **Token Refresh:** `POST /api/v1/auth/refresh`
- **Get User Info:** `GET /api/v1/auth/user`

#### Journal
- **Create Journal:** `POST /api/v1/journal/`
- **Get My Journals (paginated):** `GET /api/v1/journal/me`
- **Search Journals (keyword/date):** `GET /api/v1/journal/search`
- **Get Journal by ID:** `GET /api/v1/journal/{journal_id}`
- **Update Journal:** `PATCH /api/v1/journal/{journal_id}`
- **Delete Journal:** `DELETE /api/v1/journal/{journal_id}`
- **Generate Presigned URL for Image Upload:** `POST /api/v1/journal/{journal_id}/image`
- **Complete Image Upload:** `POST /api/v1/journal/{journal_id}/image/complete`
- **Generate AI Image for Journal:** `POST /api/v1/journal/image/generate`
- **Analyze Journal (keyword & emotion association):** `POST /api/v1/journal/{journal_id}/analyze`

#### Self‑Aware
- **Get Question by ID:** `GET /api/v1/self-aware/question/{question_id}`
- **Get Today’s Question (creates if not exists):** `GET /api/v1/self-aware/question`
- **Submit Answer:** `POST /api/v1/self-aware/answer`
- **Get Q&A History (paginated):** `GET /api/v1/self-aware/QA-history`
- **Get Value Map (by user):** `GET /api/v1/self-aware/value-map`
- **Get Top Value Scores (by user):** `GET /api/v1/self-aware/top-value-scores`
- **Get Personality Insight (by user):** `GET /api/v1/self-aware/personality-insight`

#### Statistics
- **Emotion Rate:** `GET /api/v1/statistics/emotion-rate`  
  Returns aggregated emotion ratios across all journal entries of the user.

#### Analysis
- **Get User Type:** `GET /api/v1/analysis/user-type`: Returns the user’s personality type

- **Get Comprehensive Analysis:** `GET /api/v1/analysis/comprehensive-analysis`: Returns a Five‑Factor‑Model–based personality analysis (Conscientiousness, Neuroticism, Extraversion, Openness, Agreeableness) using the user's self‑aware Q&A data.

- **Get Today’s Personalized Advice:** `GET /api/v1/analysis/personalized-advice`: Returns a unique daily advice message (once per day) grounded in the user’s reflective Q&A history and personality insights.

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

- **Get Today’s Self-Aware Question**
```bash
curl -X GET http://localhost:3000/api/v1/self-aware/question \
-H "Authorization: Bearer <your_access_token>" \
-H "Content-Type: application/json"
```
This endpoint returns today’s reflective self-aware question.  
If a question for today doesn’t exist yet, a new one will be automatically by AI created and returned.

### Additional Examples (Statistics & Analysis)

- **Get Emotion Rate (Statistics)**
```bash
curl -X GET http://localhost:3000/api/v1/statistics/emotion-rate \
-H "Authorization: Bearer <your_access_token>"
```

- **Get User Type (Analysis)**
```bash
curl -X GET http://localhost:3000/api/v1/analysis/user-type \
-H "Authorization: Bearer <your_access_token>"
```

- **Get Comprehensive Analysis**
```bash
curl -X GET http://localhost:3000/api/v1/analysis/comprehensive-analysis \
-H "Authorization: Bearer <your_access_token>"
```

- **Get Today’s Personalized Advice**
```bash
curl -X GET http://localhost:3000/api/v1/analysis/personalized-advice \
-H "Authorization: Bearer <your_access_token>"
```



---

## 🖥️ Environment

This demo is built using the following technologies:

- **FastAPI:** Modern, fast (high-performance) web framework for building APIs with Python 3.10+
- **MySQL:** Robust relational database for storing user data
- **Docker & Docker Compose:** Containerization for consistent development environment
- **JWT Authentication:** Secure, stateless user authentication using JSON Web Tokens
- **OpenAI API:** For AI-powered keyword extraction and reflection analysis

---

## 📜 Notes

This demo serves as a proof-of-concept backend system designed for integration with an Android application.  
It currently provides a solid foundation for:
  
- Implemented secure user authentication flows (Signup, Login, Refresh)
- Managing user sessions with JWT tokens
- Journal CRUD + image upload + AI summarization & keyword extraction
- Self-Aware Q&A features including value extraction
- Statistics & Analysis API
    - Emotion trends, keyword summaries, ValueMap aggregation
    - Personality insights (Five-Factor), daily personalized advice
- Android integration for all features
- EC2 deployment and stable communication between client & server
- Extensive Unit & Integration Test coverage (Journal, Self-Aware, Analysis, Statistics)

Next steps include:

- Tutorials Page
    - Onboarding-style walkthrough
    - For new users to understand Journals, Self-Aware, Statistics, and Analysis features
- Settings Screen
- Refactoring & Final Testing
    - Clean up legacy code
	- Increase test coverage across Android & Backend
	- API response consistency audit
	- UI/UX polishing (animation, error states, empty states)

---

## 🎥 Demo Video

You can watch the working demo below:

[▶️ Demo Videos](https://drive.google.com/file/d/1UvCSZwnp8PLHQysFvRSObGxfgmJsh1uf/view?usp=share_link)

> 💡 If the video doesn’t automatically play, right-click the link and choose **“Open in new tab”** or **“Save link as…”** to download it.
