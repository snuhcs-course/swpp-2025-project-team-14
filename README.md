# SWPP Team 14 - MindLog : AI-Powered Self-Reflective Journal App
<img height="200" alt="application_logo" src="https://github.com/user-attachments/assets/90b6e3ca-2e3a-4b2d-add4-0a610a129f67" />

MindLog is an AI-powered journaling application that helps users record daily emotions, reflect through psychologically grounded self-aware questions, and receive personality-based insights and personalized advice.
The system integrates secure authentication, journal management, AI analysis, emotion statistics, and personality profiling into a unified Android & FastAPI-based platform.

---

## ✨ Core Features

### 🔐 User Authentication & Profile
- Secure **JWT-based Signup / Login / Logout**
- **Access & Refresh Token** management
- Persistent login & automatic token refresh
- User profile editing
- Secure password update

### 📝 Journal System
- Create, edit, delete, and view journals
- Record **10-dimensional emotion intensities**
- Keyword-based & date-based journal search
- AI-based:
  - Emotion & keyword extraction
  - Keyword-emotion association analysis
- Image features:
  - Secure image upload via **AWS S3 Presigned URL**
  - AI-based image generation from journal content

### 🧘 Self-Aware Reflection System
- Daily AI-generated **self-aware questions**
- User answer storage & history tracking
- **ValueMap & ValueScore extraction** from psychological models
- Five-Factor Model based personality data generation

### 📊 Statistics & Visualization
- Emotion ratio distribution
- Emotion trend over time
- Event extraction contributing to specific emotions
- Keyword frequency **WordCloud analytics**

### 🔍 Personality Analysis & AI Feedback
- **User Personality Type classification**
- **Five-Factor Model (FFM) Analysis**
- Daily AI-generated **personalized advice**
- Analysis grounded in:
  - Five-Factor Model (FFM)
  - IPIP-NEO-PI personality indicators

---

## 🏗 System Architecture

- **Frontend:** Android (Kotlin, MVVM + Clean Architecture)
- **Backend:** FastAPI (Python 3.10+)
- **Database:** MySQL
- **Authentication:** JWT Access & Refresh Tokens
- **AI Integration:** OpenAI API (LangChain)
- **Image Storage:** AWS S3
- **Deployment:** Docker + AWS EC2
- **Testing:** Pytest, JUnit, Espresso, Mockito

---

## 🚀 How to Run Locally

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

SDK Info:
- Minimum SDK: 26
- Target SDK: 34

---

## ☁️ Deployment (AWS EC2)

- You can access the FastAPI Swagger UI at:
  ```
  http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/docs
  ```
- If the page loads successfully, the backend is running properly.

---

## 🧩 Detail Features

Below is the detail of key features:

- **User Signup:** Register new users with secure password handling.
- **User Login:** Authenticate users and issue JWT access and refresh tokens.
- **Logout Handling:** Invalidate refresh tokens to securely log users out.
- **Token Refresh:** Automatically refresh access tokens using refresh tokens to keep users logged in.
- **Access Token Verification:** Validate and verify access tokens for secure API access.
- **User Information Retrieval:** Fetch authenticated user details using JWT-based access control.
- **Update user profile:** Update the currently authenticated user data and retrieve info.
- **Update user password:** Update the currently authenticated user password.

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

- **Self-Aware Question & Answer:**  Interactive Q&A system that prompts users with reflective psychological questions after journaling to enhance self-awareness.
- **ValueMap & ValueScore Extraction:**  Based on the user's answers to questions, producing value map based on psychological model(five factor model) and user's core values keywords.
- **Question/Answer History:**  All self-aware interactions are stored and can be revisited.

These features form the selfaware feature which app interacts with user after journal writing.

- **Emotion Rates:** Ratio-based distribution of user emotions aggregated from journal entries
- **Emotion Trends:** Time-series visualization of emotional changes of all emotions or selected emotion.
- **Emotion Events:** Summarize events that cause certain types of emotions.
- **Keyword WorldCloud:** Visualization of frequently occurring keywords in journal history.

This feature provide actual values for statistic feature.

- **User Type:** Personality type classification based on extracted Big Five trait distributions of Five Factor Model.
- **Comprehensive Analysis:** Personality insights based on Five-Factor model of personality (Openness, Conscientiousness, Extraversion, Agreeableness, and Neuroticism).
- **Personalized Daily Advice:** AI-generated daily feedback and guidance tailored to the user’s personality profile.

This feature form analysis feature which gives more accurate psychological insight of user based on authorized theory and its method; FFM and IPIP-NEO-PI.

- **End-to-End Flow:** Demonstration of how journal data and self-aware answers are combined into statistical summaries and psychological analysis, fully integrated with the Android frontend.

---

### API Overview

#### Auth
- **User Signup:** `POST /api/v1/auth/signup`
- **User Login:** `POST /api/v1/auth/login`
- **User Logout:** `POST /api/v1/auth/logout`
- **Token Refresh:** `POST /api/v1/auth/refresh`
- **Get User Info:** `GET /api/v1/auth/user`
- **Update User Profile:** `PATCH /api/v1/user/me`
- **Update User Password:** `PATCH /api/vi/user/update-password`

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


## 🎥 Demo Video

You can watch the working demo below:

[▶️ Demo Videos](https://drive.google.com/file/d/1Xdco83sr80qsVEQAtJbNjE5fvYkctAys/view?usp=sharing)

> 💡 If the video doesn’t automatically play, right-click the link and choose **“Open in new tab”** or **“Save link as…”** to download it.

---

## 📘 Academic Background

- Expressive emotional journaling interventions[^1],[^2]
- Five-Factor Model (FFM)[^3]
- IPIP-NEO-PI personality indicators[^4],[^5],[^6]
- CBT-based cognitive restructuring[^7]
- ACT-based acceptance and value-driven behavior change[^8]
- Emotional intelligence theory[^9]

[^1]: Pennebaker, J. W., & Chung, C. K. (2011). *Expressive writing: Connections to physical and mental health*. Oxford Handbook of Health Psychology.
[^2]: Baikie, K. A., & Wilhelm, K. (2005). *Emotional and physical health benefits of expressive writing*. Advances in Psychiatric Treatment, 11(5), 338–346.
[^3]: McCrae, R. R., & Costa, P. T. (1987). *Validation of the five-factor model of personality across instruments and observers*. Journal of Personality and Social Psychology, 52(1), 81–90.
[^4]: Goldberg, L. R., Johnson, J. A., Eber, H. W., Hogan, R., Ashton, M. C., Cloninger, C. R., & Gough, H. C. (2006). *The International Personality Item Pool and the future of public-domain personality measures*. Journal of Research in Personality, 40(1), 84–96.
[^5]: Johnson, J. A. (2014). *Measuring thirty facets of the Five Factor Model with a 120-item public domain inventory: Development of the IPIP-NEO-120*. Journal of Research in Personality, 51, 78–89.
[^6]: Maples, J. L., Guan, L., Carter, N. T., & Miller, J. D. (2014). *A test of the International Personality Item Pool representation of the revised NEO Personality Inventory and development of a 120-item IPIP-based measure of the Five-Factor Model*. Psychological Assessment, 26(4), 1070–1084.
[^7]: Beck, A. T. (1976). *Cognitive Therapy and the Emotional Disorders*. New York: International Universities Press.
[^8]: Hayes, S. C., Strosahl, K. D., & Wilson, K. G. (1999). *Acceptance and Commitment Therapy: An experiential approach to behavior change*. New York: Guilford Press.
[^9]: Salovey, P., & Mayer, J. D. (1990). *Emotional intelligence*. Imagination, Cognition and Personality, 9(3), 185–211.
