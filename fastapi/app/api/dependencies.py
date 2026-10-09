from typing import Annotated
from uuid import UUID

from fastapi import Header

AiRequestId = Annotated[
    UUID,
    Header(alias="X-AI-Request-ID"),
]