from fastapi import FastAPI

from app.api.insight import router as insight_router
from app.api.matching import router as matching_router
from app.api.record_analysis import router as record_analysis_router

app = FastAPI(title="Dortfolio AI Service")

app.include_router(matching_router)
app.include_router(record_analysis_router)
app.include_router(insight_router)


@app.get("/health")
def health():
    return {"status": "ok"}