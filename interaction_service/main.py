import json
from io import BytesIO
from typing import Optional, Union

import requests
import yaml
from fastapi import FastAPI
from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse
from requests import PreparedRequest, Request, Response, Session

from dtos import (ExtractionJobsResponse, ExtractionResultUserResponse,
                  ExtractionStatusUserResponse, ExtractionUserRequest,
                  ExtractionUserResponse, ForwardingUserRequest,
                  ForwardingUserResponse, SettingsResponse, StatusResponse)
from models import (ExtractionServiceFastAPIException,
                    ExtractionServiceHTTPException, SentExtractionRequest,
                    SettingsFileFormatException)

SETTINGS_FILE = "./service-settings.yml"

job_id_not_found_text = "unknown job id"
model_status_resp = {"model": StatusResponse}

extraction_services_mapping: dict[str, str] = {}
extraction_requests: dict[str, SentExtractionRequest] = {}

app = FastAPI(title="HTTP Interaction Service for Schema Extraction of multiple NoSQL Databases",
              version="1.0.0")


@app.on_event("startup")
async def startup_event():
    """ load the service settings file """
    try:
        with open(SETTINGS_FILE, "r") as f:
            yaml_settings: dict = yaml.safe_load(f)
            temp_settings = yaml_settings.get("extraction-services")
            if temp_settings is not None and type(temp_settings) == dict:
                extraction_services_mapping.update(temp_settings)
                print(
                    f"INFO: loaded following prefixes and service urls: {extraction_services_mapping}")
            else:
                raise SettingsFileFormatException(
                    "yaml config found, but content of 'extraction-services' is not in the expected format")
            temp_meta_url = yaml_settings.get("meta-model-url")
            global meta_model_url
            if temp_meta_url is not None and type(temp_meta_url) == str and not temp_meta_url.isspace():
                meta_model_url = temp_meta_url
                print(
                    f"INFO: loaded following meta model url: {meta_model_url}")
            else:
                raise SettingsFileFormatException(
                    "yaml config found, but content of 'meta-model-url' is not in the expected format")
    except Exception as e:
        print(f"error while reading the settings file ({SETTINGS_FILE}):\n")
        print(e)
        print(f"\nplease provide a valid {SETTINGS_FILE} file, for example:")
        print("""---
meta-model-url: http://localhost:8084

# add your connections string prefixes and the corresponding service urls
extraction-services:
  mongodb: http://localhost:8000
  neo4j: http://localhost:7000
""")
        exit()


@app.exception_handler(ExtractionServiceFastAPIException)
async def custom_599_exception_handler(request: Request, exc: ExtractionServiceFastAPIException) -> JSONResponse:
    return JSONResponse(
        status_code=599,
        content=jsonable_encoder(StatusResponse(status=f"{exc}")),
    )


@app.get("/status", summary="returns the service status")
async def read_status() -> StatusResponse:
    return StatusResponse(status="running")


@app.get("/settings", summary="returns the default settings")
async def read_settings() -> SettingsResponse:
    return SettingsResponse(meta_model_service=meta_model_url, extraction_services=extraction_services_mapping)


@app.post("/extract", summary="selects the matching service and sends the schema extraction request", response_model=ExtractionUserResponse,
          responses={400: model_status_resp, 501: model_status_resp, 599: model_status_resp})
async def start_schema_extraction(req_in: ExtractionUserRequest):
    """ selects the matching extraction service, sends the request to it and returns the job id """

    extraction_service_to_call = ""
    if req_in.extraction_service is not None and req_in.extraction_service.strip() != "":
        if not req_in.extraction_service.strip().startswith("http"):
            return JSONResponse(status_code=400,
                                content=jsonable_encoder(StatusResponse(status="please provide a valid extraction service url in the request. for example 'http://myservice:1234'")))
        extraction_service_to_call = req_in.extraction_service.strip()
    else:
        service_url = find_matching_service_url(req_in.uri)
        if service_url is None:
            return JSONResponse(status_code=501,
                                content=jsonable_encoder(StatusResponse(status="failed to find a matching prefix for your database connection string, please provide an extraction service url in the request or update the service-settings.yml file")))
        extraction_service_to_call = service_url

    req = Request(method="POST", url=modify_url_to_api_call(extraction_service_to_call, "extract"), json={
        "uri": req_in.uri, "user": req_in.user, "password": req_in.password})
    resp = send_request_to_extraction_service(
        service_base_url=extraction_service_to_call, api_call=req.prepare())

    resp_json: dict = resp.json()
    raw_job_id = resp_json.get("job_id")
    if raw_job_id is None:
        return JSONResponse(status_code=599,
                            content=jsonable_encoder(StatusResponse(status=f"start extraction call was not successful. job id field is missing in response. extraction service response is: status: {resp.status_code}, body: {resp.text}")))

    extraction_requests[raw_job_id] = SentExtractionRequest(
        extraction_service_url=extraction_service_to_call, received_job_id=raw_job_id)
    return ExtractionUserResponse(extraction_service=extraction_service_to_call, job_id=raw_job_id)


@app.get("/jobs", summary="get all job ids")
async def read_all_jobs() -> ExtractionJobsResponse:
    return ExtractionJobsResponse(jobs=list(extraction_requests.keys()))


@app.get("/jobs/{job_id}", summary="read job status from schema extraction service", response_model=ExtractionStatusUserResponse,
         responses={404: {"model": ExtractionStatusUserResponse}, 599: model_status_resp})
async def read_job_status(job_id: str):
    sent_req = extraction_requests.get(job_id)
    if sent_req is None:
        return JSONResponse(status_code=404,
                            content=jsonable_encoder(ExtractionStatusUserResponse(job_id=job_id, extraction_service="none",
                                                                                  status=job_id_not_found_text)))

    req = Request(method="GET", url=modify_url_to_api_call(
        sent_req.extraction_service_url, f"jobs/{sent_req.received_job_id}"))
    resp = send_request_to_extraction_service(
        service_base_url=sent_req.extraction_service_url, api_call=req.prepare())
    resp_json: dict = resp.json()
    if resp_json.get("status") is None:
        return JSONResponse(status_code=599,
                            content=jsonable_encoder(StatusResponse(status=f"job status call was not successful. status field is missing in response. extraction service response is: http status: {resp.status_code}, body: {resp.text}")))

    return ExtractionStatusUserResponse(job_id=sent_req.received_job_id, extraction_service=sent_req.extraction_service_url, status=resp_json.get("status"))


@app.get("/jobs/{job_id}/results", summary="read job results from schema extraction service", response_model=ExtractionResultUserResponse,
         responses={404: {"model": ExtractionStatusUserResponse}, 409: {"model": ExtractionStatusUserResponse}, 599: model_status_resp})
async def read_job_results(job_id: str):
    sent_req = extraction_requests.get(job_id)
    if sent_req is None:
        return JSONResponse(status_code=404,
                            content=jsonable_encoder(ExtractionStatusUserResponse(job_id=job_id, extraction_service="none",
                                                                                  status=job_id_not_found_text)))

    req = Request(method="GET", url=modify_url_to_api_call(
        sent_req.extraction_service_url, f"jobs/{sent_req.received_job_id}/results"))
    resp = send_request_to_extraction_service(
        service_base_url=sent_req.extraction_service_url, api_call=req.prepare(), accepted_status_codes=[200, 409])
    if resp.status_code == 409:
        return JSONResponse(status_code=409,
                            content=jsonable_encoder(ExtractionStatusUserResponse(job_id=sent_req.received_job_id, extraction_service=sent_req.extraction_service_url, status=resp.json().get("status"))))

    resp_json: dict = resp.json()
    if resp_json.get("json_schema") is None:
        return JSONResponse(status_code=599,
                            content=jsonable_encoder(StatusResponse(status=f"job results call was not successful. json_schema field is missing in response. extraction service response is: status: {resp.status_code}, body: {resp.text}")))

    return ExtractionResultUserResponse(job_id=sent_req.received_job_id, extraction_service=sent_req.extraction_service_url, json_schema=resp_json.get("json_schema"))


@app.post("/jobs/{job_id}/forward", summary="read job results and forward to meta model service", response_model=ForwardingUserResponse, responses={404: {"model": ExtractionStatusUserResponse}, 409: {"model": ExtractionStatusUserResponse}, 599: model_status_resp})
async def forward_job_results(job_id: str, req_in: Union[ForwardingUserRequest, None] = None):
    """ basically like read_job_results but with forwarding to the MetaModel service """
    sent_req = extraction_requests.get(job_id)
    if sent_req is None:
        return JSONResponse(status_code=404,
                            content=jsonable_encoder(ExtractionStatusUserResponse(job_id=job_id, extraction_service="none",
                                                                                  status=job_id_not_found_text)))
    meta_model_service_base_url = meta_model_url
    if req_in is not None and req_in.meta_model_service is not None and req_in.meta_model_service.strip() != "":
        meta_model_service_base_url = req_in.meta_model_service

    req = Request(method="GET", url=modify_url_to_api_call(
        sent_req.extraction_service_url, f"jobs/{sent_req.received_job_id}/results"))
    resp = send_request_to_extraction_service(
        service_base_url=sent_req.extraction_service_url, api_call=req.prepare(), accepted_status_codes=[200, 409])
    if resp.status_code == 409:
        return JSONResponse(status_code=409,
                            content=jsonable_encoder(ExtractionStatusUserResponse(job_id=sent_req.received_job_id, extraction_service=sent_req.extraction_service_url, status=resp.json().get("status"))))

    resp_json: dict = resp.json()
    if resp_json.get("json_schema") is None:
        return JSONResponse(status_code=599,
                            content=jsonable_encoder(StatusResponse(status=f"job results call was not successful. json_schema field is missing in response. extraction service response is: status: {resp.status_code}, body: {resp.text}")))

    in_memory_buffer = BytesIO(
        bytes(json.dumps(resp_json.get("json_schema")), encoding="UTF-8"))
    files = {
        "files": (f"{sent_req.received_job_id}.schema.json", in_memory_buffer),
    }
    try:
        response = requests.post(
            f"{meta_model_service_base_url}/files/uploadFiles", files=files)
        if response.status_code == 200:
            return ForwardingUserResponse(forwarded_file=f"{sent_req.received_job_id}.schema.json")
        else:
            raise ExtractionServiceHTTPException(
                f"meta model service response was not 200 OK. it was {response.status_code}, body: {response.text}")
    except Exception as e:
        return JSONResponse(status_code=599, content=jsonable_encoder(StatusResponse(status=f"file upload to meta model service failed with: {e}")))


def send_request_to_extraction_service(service_base_url: str, api_call: PreparedRequest, accepted_status_codes: list[int] = [200]) -> Response:
    """ sends a status request to the extraction service and after that the real api call

        returns the requests.Response object for the real api call

        or raises a ExtractionServiceFastAPIException (no need to catch, because FastAPI will handle it, see exception_handler)
    """
    extraction_service_available = False
    extraction_service_api_status_running = False
    extraction_service_api_call_okay = False

    try:
        resp = requests.get(url=modify_url_to_api_call(
            service_base_url, "status"))
        extraction_service_available = True
        resp_json: dict = resp.json()
        if resp_json.get("status") != "running":
            raise ExtractionServiceHTTPException(
                "status of extraction service is not running, it was: " + resp_json.get("status"))
        extraction_service_api_status_running = True

        s = Session()
        resp = s.send(api_call)
        extraction_service_api_call_okay = resp.status_code in accepted_status_codes
        if not extraction_service_api_call_okay:
            raise ExtractionServiceHTTPException(
                f"response of extraction service for route '{api_call.url}' is not in {accepted_status_codes}, it was: {resp.status_code}, body: {resp.text}")
        return resp
    except Exception as e:
        # except all possible Exceptions (session broken, ssl errors or our ExtractionServiceHTTPException) and wrap it to a FastAPI-known class
        raise ExtractionServiceFastAPIException(
            f"extraction service available: {extraction_service_available}. extraction service implements status api: {extraction_service_api_status_running}. extraction service implements api: {extraction_service_api_call_okay}. error was: {e}")


def find_matching_service_url(db_url: str) -> Optional[str]:
    """ trys to find a matching Extraction Service URL """
    # print(f"try to find a extraction service for '{db_url}'")
    for key in extraction_services_mapping.keys():
        if db_url.startswith(key):
            return extraction_services_mapping.get(key).strip()
    return None


def modify_url_to_api_call(db_url: str, api_route: str) -> str:
    if db_url.endswith("/"):
        return f"{db_url}{api_route}"
    else:
        return f"{db_url}/{api_route}"
