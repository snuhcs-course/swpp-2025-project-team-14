from fastapi import FastAPI


app = FastAPI(title='MindLog')

@app.get("/")
def read_root():
    return {"message": "Hello World!"}
