from fastapi import FastAPI

from app.api.record_analysis import router as record_analysis_router

app = FastAPI(title="Dortfolio AI Service")

app.include_router(record_analysis_router)


@app.get("/health")
def health():
    return {"status": "ok"}
