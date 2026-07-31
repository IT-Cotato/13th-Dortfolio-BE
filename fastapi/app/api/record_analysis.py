from fastapi import APIRouter

from app.schemas.record_analysis import RecordAnalysisRequest, RecordAnalysisResponse
from app.services.record_analysis_service import analyze_record

router = APIRouter(prefix="/ai/records", tags=["record-analysis"])


@router.post("/analyze", response_model=RecordAnalysisResponse)
def analyze_record_endpoint(request: RecordAnalysisRequest):
    return analyze_record(request)
