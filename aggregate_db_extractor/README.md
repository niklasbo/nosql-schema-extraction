# HTTP Service for Aggregate-oriented Databases

This service makes the schema extraction for aggregate-oriented databases available via HTTP.

## Run the Development version
Uses `maven` and Java 11 or higher.
Start the `App.java` with your IDE or in shell with:
```
java -cp path\to\workspaceStorage\bin' 'de.fernunihagen.App'
```
The service should be available on port 7000.

## Build JAR locally
You can build the service with:
```
mvn install -f ./pom.xml
mvn package -f ./pom.xml

# or clean target directory before the new build:
mvn clean package -f ./pom.xml
```

`app.jar` and `original-app.jar` files should be in the `./target/` directory.
The file `app.jar` is the *bundled/shaded/fat* jar with all dependencies.
The file `original-app.jar` is the slim jar that has no dependencies included. 

## Build with Docker

Use the Dockerfile to build a container image with `docker build --no-cache -t aggregatedb-extraction-service .`.

Run the Dockerimage as a detached container on port 7000 with `docker run -d --name aggregatedb-extraction-service-instance-1 -p 7000:7000 aggregatedb-extraction-service`.

For more details see the Multi-Stage Dockerfile in this directory.  

## API-Docs

You can find the api definition in this directory. It is the same as the graph db service.

## Allowed Database Connection Strings / URIs
__MongoDB__ uri strings are defined by the official documentation: https://www.mongodb.com/docs/manual/reference/connection-string/.
```
mongodb://example.com:27017/mydatabase

mongodb://host.docker.internal:27017/mydatabase --> docker hosts
```

~~__CouchDB__ uri strings should look like this:~~
```
couchdb://mycooldomain:7667/mydatabase
couchdb://127.0.0.1:7667/mydatabase
couchdb://localhost/mydatabase     --> default port is 5984

couchdb://host.docker.internal:5984/posts --> docker hosts
```

(!) Otherwise it is not possible to parse the connection string

# Changes to the original version by Chillón et al.

The packages `es.um.nosql.s13e.*` are copied from https://github.com/catedrasaes-umu/NoSQLDataEngineering

Following changes were made:
- changed a lot in MongoDBImport to use our Connection String and new driver
- updated the package to be container-executable, i.e. removed file dependencies to `map.js`/`reduce.js` and other local files
- changed CouchDBImport to use the CouchDbProperties in constructor
- changed the output format to NoSQLSchema instead of direct export to xml (See file BuildNoSQLSchema.java.buildFromGsonArray)
- updated dependencies for security reasons: com.google.code.gson
- renamed a couple of variable names
- added an Interface IDatabaseImport to generalize the MongoDBImport / CouchDBImport classes
- changed driver version to connect to new mongodb versions
- fixed XML file writing (close the file afterwards)
- changed variable names from "var" to not keyword names
- Not possible to use MongoDB Atlas: com.mongodb.MongoCommandException: Command failed with error 8000 (AtlasError): 'CMD_NOT_ALLOWED: mapreduce' on server ac-4mkwvcf-shard-00-01.zcid0rp.mongodb.net:27017. The full response is {"ok": 0, "errmsg": "CMD_NOT_ALLOWED: mapreduce", "code": 8000, "codeName": "AtlasError"}

# MongoDB

## MongoDB general note

Normal instances of MongoDB are supported, tested up to MongoDB version 6.0.x.

MongoDB Atlas cloud instances are not supported.

## Setup local MongoDB
Download ZIPs for MongoDB and MongoDB Command Line Database Tools.

Extract the files and execute following in a terminal:
```
cd to/mongodb/bin
.\mongod.exe --dbpath="c:\Users\Niklas\Downloads\mongodb-6.0.4\mydata"
```

Open a second terminal, disable telemetry and create a new database `mflix`:
```
cd to/mongodbshell/bin
.\mongosh.exe "mongodb://localhost:27017"
disableTelemetry()
db.disableFreeMonitoring()
use mflix
exit
```

Now load the sample dataset `mflix` (https://github.com/neelabalan/mongodb-sample-dataset) into the database:
```
cd to/mongodbtools/bin
.\mongoimport.exe --drop --db mflix --collection comments --file "path/to/mflix/comments.json"
.\mongoimport.exe --drop --db mflix --collection sessions --file "path/to/mflix/sessions.json"
.\mongoimport.exe --drop --db mflix --collection theaters --file "path/to/mflix/theaters.json"
.\mongoimport.exe --drop --db mflix --collection users --file "path/to/mflix/users.json"
```

# CouchDB

## CouchDB general note

CouchDB versions < 2.0 are supported, because tempViews were removed in major version 2.

It is not recommended to use this service for CouchDBs.

I changed a lot in the code to have a sometimes working extraction, but it works only when for example each object in the database have special fields.

For normal, unedited databases the following error appears and the extraction process stops:
```
ma_prototype-aggregate_db_extractor-1  | java.lang.IllegalArgumentException: JSON rows do not follow the expected schema: [ {schema: <JSON Object>, count: <Integer>, timestamp: <Long>} 
...]
ma_prototype-aggregate_db_extractor-1  |        at es.um.nosql.s13e.json2dbschema.process.SchemaInference.<init>(SchemaInference.java:54)
ma_prototype-aggregate_db_extractor-1  |        at es.um.nosql.s13e.json2dbschema.main.util.JSON2Schema.fromJSONArray(JSON2Schema.java:52)
ma_prototype-aggregate_db_extractor-1  |        at es.um.nosql.s13e.json2dbschema.main.BuildNoSQLSchema.buildFromArrayNBEdit(BuildNoSQLSchema.java:60)
ma_prototype-aggregate_db_extractor-1  |        at es.um.nosql.s13e.json2dbschema.main.BuildNoSQLSchema.buildFromGsonArrayNBEdit(BuildNoSQLSchema.java:45)
ma_prototype-aggregate_db_extractor-1  |        at de.fernunihagen.ExtractionWorkerThread.run(ExtractionWorkerThread.java:65)
```

## Setup a legacy CouchDB v1.7 with Docker
```
docker run -d --name couchdb-instance-1 -p 5984:5984 -e COUCHDB_USER=couchdb -e COUCHDB_PASSWORD=couchdb couchdb:1.7
```
Open the Web UI `http://127.0.0.1:5984/_utils`.

## Additional Steps for CouchDB 3.x
Visit the Web UI `http://127.0.0.1:5984/_utils/#/setup` and login with the admin user.

Setup CouchDB as a single node instance. (You can also use a CouchDB cluster, but for local testing it is easier.)
`http://127.0.0.1:5984/_utils/#setup/singlenode`

## Add a document to CouchDB for testing
Create in the Web UI a new 'database', for example `posts` and insert following document:
```
{
  "_id": "c8d1bd3a4540a1a533aac52e33000cb0",
  "username": "userino",
  "text": "my first post",
  "create_ts": "2023-04-08T18:30:00+02:00",
  "reactions": [
    {
      "username": "a_username",
      "time": "2023-04-08T18:35:00+02:00",
      "reaction": "like"
    },
    {
      "username": "another_user",
      "time": "2023-04-08T18:40:00+02:00",
      "reaction": "love"
    }
    ]
}
```
