class SettingsFileFormatException(Exception):
    pass


class ExtractionServiceHTTPException(Exception):
    pass


class ExtractionServiceFastAPIException(Exception):
    pass


class SentExtractionRequest(object):
    """ Internal queue item for the extraction """
    extraction_service_url: str
    received_job_id: str

    def __init__(self, extraction_service_url: str, received_job_id: str):
        self.received_job_id = received_job_id
        self.extraction_service_url = extraction_service_url
