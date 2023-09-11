# Prototypes for Schema Extraction of different NoSQL-Databases

## Overview

You can find the following microservices in the subdirectories:

- Interaction Service:
This service provides an API for normal users. You can start schema extractions and request the results.
In the service-settings.yml file you can add/modify schema extraction services.
You can view the OpenAPI specification directly in the browser (`/docs`).

- Graph DB Extractor:
This service enables you to extract database schemas from Neo4j graph databases.
Cloud instances of Neo4j AuraDB are also supported.
You can view the OpenAPI specification directly in the browser (`/docs`).

- Aggregate DB Extractor:
This service enables you to extract database schemas from MongoDB and CouchDB databases.
Cloud instances of MongoDB Atlas are not supported, because they do not support map-reduce operations.
You can view the OpenAPI specification in the subdirectory, it is equal to the API of the Graph DB Extractor.

## Run it

Dependencies:
- Docker: tested with Docker Engine v20.10.24
- docker-compose: tested with Docker Compose version v2.17.2

Default Ports:
- `80` for the Interaction Service
- `7000` for the Aggregate DB Extractor
- `8000` for the Graph DB Extractor

Start the services with the `docker-compose.yml` and the `Dockerfile`s in subdirectories.
```
# attached to the current terminal, recommended while testing (to view all logs)
docker-compose up

# in detached mode
docker-compose up -d
``` 

### What can I do with the services?

Quickstart your tests with the exported [API Tests](./api_tests_insomnia_v4_export.json) for the open-source API Testing Tool [Insomnia](https://insomnia.rest/products/insomnia). You can directly import the `api_tests_insomnia_v4_export.json` inside Insomnia.

*or* 

You can take a look at the [Request Protocol](./RequestProtocol.md). This protocol contains curl commands and their responses.

*or* 

View the OpenAPI specs in the subdirectories and use your preferred tools. 

### Where are the extracted database schemas?

In the `docker-compose.yml` file the results are mapped to the current directory of the docker host.
So, after the first extraction request a `./results/` directory should be created.
In this directory you find all successfully extracted database schemas, sorted in subdirectories by their job id.
