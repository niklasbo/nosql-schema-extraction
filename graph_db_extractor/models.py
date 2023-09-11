

class ExtractionQueueItem(object):
    """ Internal queue item for the extraction """
    job_id: str
    uri: str
    user: str
    password: str

    def __init__(self, job_id: str, uri: str, user: str, password: str):
        self.job_id = job_id
        self.uri = uri
        self.user = user
        self.password = password
