# HTTP Interaction Service for Schema Extraction of multiple NoSQL Databases

This service handles the user interaction with the different extraction services.

It is configured via a JSON file and proxies the extraction requests to the correct extraction service.

## Run the Development Version
You can run the service locally with Python 3.10 or newer.

Make sure you have the pip dependencies in your virtual environment (see below) installed:
```
# create a virtual environment
python -m venv venv

# activate the venv in the terminal
.\venv\Scripts\activate (.bat|.ps1)

# install all requirements
pip install -r requirements.txt
```

Start the dev version with uvicorn:
`uvicorn main:app --port 9000 --reload`

### Extend the service
Always work with a python virtual environment, you can create one with `python -m venv <venv_name>`.
Do not forget to activate it in your IDE or shell like `<venv_name>/scripts/activate(.bat|.ps1)`.

When you add further pip dependencies, do not forget to freeze the packages in the requirements.txt file.
`pip freeze --local > requirements.txt`


## Build Service
Build the Dockerimage with `docker build -t interaction-service .`.

Run the Dockerimage as a detached container on port 80 with `docker run -d --name interaction-service-instance-1 -p 80:9000 interaction-service`.

For more details see the Dockerfile in this directory.

## API-Docs

Start the service and navigate to route `/docs`.

A local copy of the file is also in this directory.
