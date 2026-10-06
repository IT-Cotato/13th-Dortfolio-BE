from pydantic import BaseModel, Field


class GenerationMetadata(BaseModel):
    promptVersion: str = Field(min_length=1, max_length=100)
    schemaVersion: str = Field(min_length=1, max_length=100)
    model: str = Field(min_length=1, max_length=200)
    maxOutputTokens: int = Field(gt=0)
    thinkingLevel: str | None = Field(default=None, max_length=20)
