# neo4j-schema-extract-code 
copied from https://github.com/MaoRodriguesJ/neo4j-schema-extract-code

Extracting a JSON-Schema to represent a Neo4j Database

Algorithm created for a graduation thesis in computer science.

---

## Changes to the original version by Salomão Rodrigues Jacinto
- changed all places where the old neo4j.v1 package was used
- changed all Cipher-Queries to new syntax | deleted `query.py` 
- changed all database calls for correct transactional/scoped usage | fixed `neo4j.exceptions.ResultConsumedError: The result is out of scope.` error
- added a debug parameter for Schema.generate() to stop verbose printing
- added Python type hints for better compiler support
- added a `__init__.py` to use this a python module
- further fixed all module import statements
- extended this README file 

## Structure
We need a `Schema` object to call the `generate`-method on it.
It will create a schema in JSON Schema.
```
s = Schema(
    Database(
        Driver(uri, user, password)))
s.generate(path)
```
