from typing import Any
from pydantic import BaseModel


class ExtractionRequest(BaseModel):
    """ DTO """
    uri: str
    user: str
    password: str


class ExtractionResponse(BaseModel):
    """ DTO """
    job_id: str


class StatusResponse(BaseModel):
    """ DTO """
    status: str


class ResultResponse(BaseModel):
    """ DTO """
    json_schema: list[dict[str, Any]]
