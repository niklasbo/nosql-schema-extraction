# HTTP Extractor Service for Graph Databases

This service makes the schema extraction for graph databases available via HTTP.

## Run the Development Version
You can run the service locally with Python 3.10 or newer.

Make sure you have the pip dependencies in your virtual environment (see below) installed:
`pip install -r requirements.txt`

Start the dev version with uvicorn:
`uvicorn main:app --port 8000 --reload`

### Extend the service
Always work with a python virtual environment, you can create one with `python -m venv <venv_name>`.
Do not forget to activate it in your IDE or shell like `<venv_name>/scripts/activate(.bat|.ps1)`.

When you add further pip dependencies, do not forget to freeze the packages in the requirements.txt file.
`pip freeze --local > requirements.txt`


## Build Service
Build the Dockerimage with `docker build -t graphdb-extraction-service .`.

Run the Dockerimage as a detached container on port 8000 with `docker run -d --name graphdb-extraction-service-instance-1 -p 8000:8000 graphdb-extraction-service`.

For more details see the Dockerfile in this directory.  

## API-Docs

Start the service and navigate to route `/docs`.

A local copy of the file is also in this directory.

## Neo4j Database Example Dataset
Movies Database:

https://raw.githubusercontent.com/neo4j-graph-examples/movies/main/scripts/movies.cypher
