from fastapi import FastAPI

app = FastAPI(title="Dortfolio AI Service")


@app.get("/health")
def health():
    return {"status": "ok"}
