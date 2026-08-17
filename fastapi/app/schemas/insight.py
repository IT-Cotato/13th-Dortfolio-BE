from uuid import UUID

from pydantic import BaseModel, Field, field_validator, model_validator


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
    matched: bool
    jobCompetencyId: UUID
    recordId: UUID | None = None
    reason: str | None = Field(default=None, max_length=300)

    @field_validator("reason")
    @classmethod
    def validate_reason(cls, value: str | None) -> str | None:
        if value is None:
            return None

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

    @model_validator(mode="after")
    def validate_match_result(self):
        if self.matched and (self.recordId is None or self.reason is None):
            raise ValueError(
                "Matched recommendation requires recordId and reason."
            )
        if not self.matched and (self.recordId is not None or self.reason is not None):
            raise ValueError(
                "Unmatched recommendation must not contain recordId or reason."
            )
        return self
