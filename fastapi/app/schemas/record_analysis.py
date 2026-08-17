from uuid import UUID

from pydantic import BaseModel


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
    description: str | None = None


class RecordAnalysisRequest(BaseModel):
    recordId: UUID
    title: str
    activity: ActivityPayload
    template: TemplatePayload
    answers: list[AnswerPayload]
    memos: list[MemoPayload]
    strengthTagCandidates: list[StrengthTagCandidatePayload]


class AnalyzedStrengthTagResponse(BaseModel):
    strengthTagId: UUID
    score: float


class RecordAnalysisResponse(BaseModel):
    summary: str
    evidenceSnippets: list[str]
    strengthTags: list[AnalyzedStrengthTagResponse]
    embeddingModel: str
    embedding: list[float]
