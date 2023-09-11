import json
import os
import queue
import threading
import time
from typing import Any

from fastapi import FastAPI
from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse

from dtos import (ExtractionRequest, ExtractionResponse, ResultResponse,
                  StatusResponse)
from extraction import database, driver, schema
from models import ExtractionQueueItem

job_id_not_found_text = "unknown job id"

state_waiting = "waiting"
state_running = "running"
state_finished = "finished"
state_aborted = "aborted"
extraction_request_states: dict[str, str] = {}

extraction_queue = queue.Queue()

app = FastAPI(title="HTTP Extractor Service for Graph Databases",
              version="1.0.0")


@app.on_event("startup")
async def startup_event():
    """ start the extraction worker to handle the extraction_queue """
    threading.Thread(target=extraction_worker, daemon=True).start()


@app.on_event("shutdown")
def shutdown_event():
    """ ensure that the execution queue is empty before the shutdown """
    extraction_queue.join()


@app.get("/status", summary="returns the service status")
async def read_status() -> StatusResponse:
    return StatusResponse(status="running")


@app.post("/extract", summary="starts the schema extraction process")
async def start_schema_extraction(req_in: ExtractionRequest) -> ExtractionResponse:
    """ creates a new job id and ExtractionQueueItem and adds it to the extraction queue """
    job_id = generate_job_id(req_in.uri)
    queue_request = ExtractionQueueItem(
        job_id, req_in.uri, req_in.user, req_in.password)
    update_request_state(job_id, state_waiting)
    extraction_queue.put(queue_request)
    return ExtractionResponse(job_id=job_id)


@app.get("/jobs/{job_id}", summary="read job status", response_model=StatusResponse, responses={404: {"model": StatusResponse}})
async def read_job_status(job_id: str):
    state = extraction_request_states.get(job_id, job_id_not_found_text)
    if state == job_id_not_found_text:
        return JSONResponse(status_code=404, content=jsonable_encoder(StatusResponse(status=state)))
    return StatusResponse(status=state)


@app.get("/jobs/{job_id}/results", summary="read job results", response_model=ResultResponse, responses={409: {"model": StatusResponse}})
async def read_job_results(job_id: str):
    state = extraction_request_states.get(job_id, job_id_not_found_text)
    if state != state_finished:
        return JSONResponse(status_code=409, content=jsonable_encoder(StatusResponse(status=state)))
    return ResultResponse(json_schema=get_results_for_job(job_id))


def generate_job_id(uri: str):
    """ generates a somewhat unique job id which is safe to be used as a directory name """
    alnum_chars = "".join(x for x in uri if x.isalnum())
    return f"job_{int(time.time())}_{alnum_chars}"


def update_request_state(job_id: str, new_state: str):
    """ standard function for log and change the extraction request state """
    print(f"job '{job_id}' is now {new_state}")
    extraction_request_states[job_id] = new_state


def get_results_dir_path(job_id: str) -> str:
    """ returns a valid system path to the job results """
    return os.path.join(".", "results", job_id)


def get_results_for_job(job_id: str) -> list[dict[str, Any]]:
    """ searches for the results directory of the job and merges all the result files """
    results_dir_path = get_results_dir_path(job_id)
    all_results = []
    for filename in os.listdir(results_dir_path):
        with open(os.path.join(results_dir_path, filename)) as f:
            all_results.append(json.loads(f.read()))
    return all_results


def extraction_worker():
    """ this function is executed in a separat thread and works on the request queue """
    while True:
        item: ExtractionQueueItem = extraction_queue.get()
        update_request_state(item.job_id, state_running)
        extract_schema(item)
        extraction_queue.task_done()


def extract_schema(req: ExtractionQueueItem):
    """ creates and starts the schema extraction for a queue item """
    try:
        d = driver.Driver(req.uri, req.user, req.password)
        s = schema.Schema(database.Database(d))
        s.generate(get_results_dir_path(req.job_id))
        d.close()
        update_request_state(req.job_id, state_finished)
    except Exception as e:
        update_request_state(req.job_id, state_aborted)
        print(f"{req.job_id}: {e}")
