from uuid import UUID

from pydantic import BaseModel, Field, field_validator, model_validator


class InsightRecommendationCandidate(BaseModel):
    recordId: UUID
    recordTitle: str
    templateName: str
    summary: str | None = None
    evidenceSnippets: list[str]
    similarity: float


class InsightRecommendationCompetency(BaseModel):
    jobCompetencyId: UUID
    competencyName: str
    competencyDescription: str | None = None
    candidates: list[InsightRecommendationCandidate]


class InsightRecommendationRequest(BaseModel):
    jobId: UUID
    jobName: str
    competencies: list[InsightRecommendationCompetency] = Field(
        min_length=5,
        max_length=5,
    )

    @model_validator(mode="after")
    def validate_unique_competencies(self):
        competency_ids = [
            competency.jobCompetencyId
            for competency in self.competencies
        ]

        if len(competency_ids) != len(set(competency_ids)):
            raise ValueError(
                "Recommendation competencies must be unique."
            )

        return self


class InsightRecommendationResult(BaseModel):
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


class InsightRecommendationResponse(BaseModel):
    recommendations: list[InsightRecommendationResult] = Field(
        min_length=5,
        max_length=5,
    )
