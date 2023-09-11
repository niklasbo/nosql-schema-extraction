# Request Protocol
In each block you find the curl request, an empty line and the result.

# Neo4j Cloud Instance + local Neo4j Extraction Service
```
curl --request POST \
  --url http://127.0.0.1:8000/extract \
  --header 'Content-Type: application/json' \
  --data '{
	"uri": "neo4j+s://72726e3b.databases.neo4j.io",
	"user": "neo4j",
	"password": "kKAU_bIOWD3Z1jgOL6HSVaArDWIfoN_xxxxxxxx"
}'

{"job_id":"job_1687602264_neo4js72726e3bdatabasesneo4jio"}
```

```
curl --request GET \
  --url http://127.0.0.1:8000/jobs/job_1687602264_neo4js72726e3bdatabasesneo4jio

{"status":"finished"}
```

```
curl --request GET \
  --url http://127.0.0.1:8000/jobs/job_1687602264_neo4js72726e3bdatabasesneo4jio/results

{"json_schema":[{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"ACTED_IN","description":"ACTED_IN relationship","$id":"acted_in.json","type":"object","properties":{"<id>":{"type":"number"},"roles":{"type":"array"}},"required":["<id>","roles"],"additionalProperties":false},{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"DIRECTED","description":"DIRECTED relationship","$id":"directed.json","type":"object","properties":{"<id>":{"type":"number"}},"required":["<id>"],"additionalProperties":false},{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"FOLLOWS","description":"FOLLOWS relationship","$id":"follows.json","type":"object","properties":{"<id>":{"type":"number"}},"required":["<id>"],"additionalProperties":false},{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"Person","description":"Person node","$id":"person.json","definitions":{},"type":"object","properties":{"<id>":{"type":"number"},"name":{"type":"string"}},"required":["<id>","name"]},{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"PRODUCED","description":"PRODUCED relationship","$id":"produced.json","type":"object","properties":{"<id>":{"type":"number"}},"required":["<id>"],"additionalProperties":false},{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"REVIEWED","description":"REVIEWED relationship","$id":"reviewed.json","type":"object","properties":{"<id>":{"type":"number"},"summary":{"type":"string"},"rating":{"type":"number"}},"required":["<id>","summary","rating"],"additionalProperties":false},{"$schema":"https://json-schema.org/draft/2019-09/schema#","title":"WROTE","description":"WROTE relationship","$id":"wrote.json","type":"object","properties":{"<id>":{"type":"number"}},"required":["<id>"],"additionalProperties":false}]}
```

# MongoDB local instance + local MongoDB Extraction Service

```
curl --request POST \
  --url http://127.0.0.1:7000/extract \
  --header 'Content-Type: application/json' \
  --data '{
	"uri": "mongodb://localhost:27017/mflix",
	"user": "",
	"password": ""
}'

{"job_id":"job_1687603116_mongodblocalhost27017mflix"}
```

```
curl --request GET \
  --url http://127.0.0.1:7000/jobs/job_1687603116_mongodblocalhost27017mflix

{"status":"finished"}
```

```
curl --request GET \
  --url http://127.0.0.1:7000/jobs/job_1687603116_mongodblocalhost27017mflix/results

{"json_schema":[{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Geo","title":"Geo","type":"object","properties":{"coordinates":{"type":"array","items":{"type":"number"}},"type":{"type":"string"}},"required":["coordinates","type"],"$id":"job_1687603116_mongodblocalhost27017mflix.Geo.schema.json"},{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Theaters","title":"Theaters","type":"object","properties":{"theaterId":{"description":"reference to job_1687603116_mongodblocalhost27017mflix.Theaters.schema.json","type":"number"},"location":{"description":"nested entity","$ref":"job_1687603116_mongodblocalhost27017mflix.Location.schema.json"},"_id":{"type":"objectid"}},"$id":"job_1687603116_mongodblocalhost27017mflix.Theaters.schema.json"},{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Address","title":"Address","type":"object","properties":{"zipcode":{"type":"string"},"city":{"type":"string"},"street1":{"type":"string"},"state":{"type":"string"},"street2":{"type":["null","string"]}},"$id":"job_1687603116_mongodblocalhost27017mflix.Address.schema.json"},{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Comments","title":"Comments","type":"object","properties":{"date":{"type":"string"},"name":{"type":"string"},"_id":{"type":"objectid"},"text":{"type":"string"},"movie_id":{"type":"objectid"},"email":{"type":"string"}},"required":["_id","date","email","movie_id","name","text"],"$id":"job_1687603116_mongodblocalhost27017mflix.Comments.schema.json"},{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Sessions","title":"Sessions","type":"object","properties":{"user_id":{"description":"reference to job_1687603116_mongodblocalhost27017mflix.Users.schema.json","type":"string"},"jwt":{"type":"string"},"_id":{"type":"objectid"}},"required":["_id","jwt","user_id"],"$id":"job_1687603116_mongodblocalhost27017mflix.Sessions.schema.json"},{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Users","title":"Users","type":"object","properties":{"password":{"type":"string"},"preferences":{"description":"nested entity","$ref":"job_1687603116_mongodblocalhost27017mflix.Preferences.schema.json"},"name":{"type":"string"},"_id":{"type":"objectid"},"email":{"type":"string"}},"$id":"job_1687603116_mongodblocalhost27017mflix.Users.schema.json"},{"$schema":"https://json-schema.org/draft/2020-12/schema","description":"Extracted and transformed schema for entity Location","title":"Location","type":"object","properties":{"geo":{"description":"nested entity","$ref":"job_1687603116_mongodblocalhost27017mflix.Geo.schema.json"},"address":{"description":"nested entity","$ref":"job_1687603116_mongodblocalhost27017mflix.Address.schema.json"}},"$id":"job_1687603116_mongodblocalhost27017mflix.Location.schema.json"}]}
```
