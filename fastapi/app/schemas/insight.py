from uuid import UUID

from pydantic import BaseModel, Field, field_validator


class InsightRecommendationCandidate(BaseModel):
    recordId: UUID
    recordTitle: str
    templateName: str
    summary: str | None = None
    evidenceSnippets: list[str]
    similarity: float


class InsightRecommendationRequest(BaseModel):
    jobId: UUID
    jobName: str
    jobCompetencyId: UUID
    competencyName: str
    competencyDescription: str | None = None
    candidates: list[InsightRecommendationCandidate] = Field(
        min_length=1
    )


class InsightRecommendationResponse(BaseModel):
    jobCompetencyId: UUID
    recordId: UUID
    reason: str = Field(min_length=1, max_length=300)

    @field_validator("reason")
    @classmethod
    def validate_reason(cls, value: str) -> str:
        normalized = value.strip()

        if not normalized:
            raise ValueError(
                "Recommendation reason must not be blank."
            )

        if "\n" in normalized or "\r" in normalized:
            raise ValueError(
                "Recommendation reason must be one line."
            )

        return normalized