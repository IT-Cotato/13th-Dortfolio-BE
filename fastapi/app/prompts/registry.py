from dataclasses import dataclass
from functools import cached_property
from pathlib import Path
from string import Template

from app.schemas.generation import GenerationMetadata


@dataclass(frozen=True)
class PromptDefinition:
    feature: str
    version: str
    schema_version: str

    @cached_property
    def template(self) -> Template:
        path = Path(__file__).parent / self.feature / f"{self.version}.txt"
        return Template(path.read_text(encoding="utf-8").strip())

    def render(self, **values) -> str:
        return self.template.substitute(values)

    def metadata(self, settings) -> GenerationMetadata:
        return GenerationMetadata(
            promptVersion=f"{self.feature}.{self.version}",
            schemaVersion=f"{self.feature}.{self.schema_version}",
            model=settings.bedrock_generation_model_id,
            maxOutputTokens=getattr(settings, f"{self.feature}_max_output_tokens"),
            # 구조화 출력을 위해 도구 호출을 강제하며, 이 방식은 확장 사고와 함께 쓸 수 없다.
            thinkingLevel=None,
        )


RECORD_ANALYSIS_PROMPT = PromptDefinition("record_analysis", "v1", "v1")
INSIGHT_RECOMMENDATION_PROMPT = PromptDefinition("insight_recommendation", "v1", "v1")
