from fastapi import FastAPI
from .features.auth.router import router as auth_router
from .features.user.router import router as user_router


app = FastAPI(title='MindLog')

app.include_router(auth_router, prefix='/api/v1')
app.include_router(user_router, prefix='/api/v1')

@app.get("/")
def read_root():
    return {"message": "Hello World!"}
