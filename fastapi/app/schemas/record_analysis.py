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


class CompetencyTagCandidatePayload(BaseModel):
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
    competencyTagCandidates: list[CompetencyTagCandidatePayload]


class AnalyzedCompetencyTagResponse(BaseModel):
    competencyTagId: UUID
    score: float


class RecordAnalysisResponse(BaseModel):
    summary: str
    evidenceSnippets: list[str]
    competencyTags: list[AnalyzedCompetencyTagResponse]
    embeddingModel: str
    embedding: list[float]
