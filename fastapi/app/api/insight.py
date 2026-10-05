from fastapi import APIRouter

from app.api.dependencies import AiRequestId
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
)
from app.services.insight_service import (
    generate_insight_recommendation,
)


router = APIRouter(
    prefix="/ai/insights",
    tags=["insight"],
)


@router.post(
    "/recommendation",
    response_model=InsightRecommendationResponse,
)
def generate_recommendation(
    request: InsightRecommendationRequest,
    request_id: AiRequestId,
) -> InsightRecommendationResponse:
    return generate_insight_recommendation(request, request_id)