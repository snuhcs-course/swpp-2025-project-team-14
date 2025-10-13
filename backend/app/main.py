from fastapi import FastAPI
from .features.auth.router import router as auth_router
from .features.user.router import router as user_router
from app.database.api.routes import answer_router, question_router, journal_router 

app = FastAPI(title='MindLog')

app.include_router(auth_router, prefix='/api/v1')
app.include_router(user_router, prefix='/api/v1')
# journal featuers
app.include_router(answer_router.router)
app.include_router(question_router.router)
app.include_router(journal_router.router)

@app.get("/")
def read_root():
    return {"message": "Hello World!"}
