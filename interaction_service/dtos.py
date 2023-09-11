from typing import Any, Optional, Union

from pydantic import BaseModel


class StatusResponse(BaseModel):
    """ DTO """
    status: str


class SettingsResponse(BaseModel):
    """ DTO """
    meta_model_service: str
    extraction_services: dict[str, str]


class ExtractionUserRequest(BaseModel):
    """ DTO """
    uri: str
    user: str
    password: str
    extraction_service: Optional[str]


class ForwardingUserRequest(BaseModel):
    """ DTO """
    meta_model_service: Union[str, None]


class ForwardingUserResponse(BaseModel):
    """ DTO """
    forwarded_file: str


class ExtractionUserResponse(BaseModel):
    """ DTO """
    job_id: str
    extraction_service: str


class ExtractionJobsResponse(BaseModel):
    jobs: list[str]


class ExtractionStatusUserResponse(BaseModel):
    """ DTO """
    job_id: str
    extraction_service: str
    status: str


class ExtractionResultUserResponse(BaseModel):
    """ DTO """
    job_id: str
    extraction_service: str
    json_schema: list[dict[str, Any]]
