from uuid import UUID

from pydantic import BaseModel, Field
from app.schemas.ai_observability import AiUsage

class ActivityPayload(BaseModel):
    title: str
    description: str | None = None


class TemplatePayload(BaseModel):
    title: str


class AnswerPayload(BaseModel):
    questionText: str
    answerText: str


class MemoPayload(BaseModel):
    title: str | None = None
    content: str


class StrengthTagCandidatePayload(BaseModel):
    id: UUID
    name: str
    description: str
    evaluationCriteria: str
    positiveExample: str
    negativeExample: str
    cosineSimilarity: float = Field(ge=-1.0, le=1.0)


class RecordAnalysisRequest(BaseModel):
    recordId: UUID
    title: str
    activity: ActivityPayload
    template: TemplatePayload
    answers: list[AnswerPayload]
    memos: list[MemoPayload]
    strengthTagCandidates: list[StrengthTagCandidatePayload]
    maxStrengthCount: int = Field(ge=1)


class RecordAnalysisResult(BaseModel):
    summary: str
    evidenceSnippets: list[str] = Field(
        min_length=1,
        max_length=5,
    )
    strengthTagIds: list[UUID]

class RecordAnalysisResponse(RecordAnalysisResult):
    usage: AiUsage
