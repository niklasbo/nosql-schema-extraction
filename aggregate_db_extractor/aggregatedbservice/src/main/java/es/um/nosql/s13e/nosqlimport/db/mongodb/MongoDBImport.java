package es.um.nosql.s13e.nosqlimport.db.mongodb;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.Document;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.fernunihagen.IDatabaseImport;
import es.um.nosql.s13e.nosqlimport.util.MapReduceSources;
import es.um.nosql.s13e.nosqlimport.util.MongoDBStreamAdapter;

public class MongoDBImport implements IDatabaseImport {
  private MongoDBStreamAdapter adapter;
  private final String connString;

  /**
   * Creates a MongoDB Import instance
   * 
   * @param connString expect a connection string in the format
   *                   mongodb://user:password@hostname:27017/mydbname. See the
   *                   full format
   *                   here
   *                   https://www.mongodb.com/docs/manual/reference/connection-string/
   * 
   */
  public MongoDBImport(String connString) {
    this.adapter = new MongoDBStreamAdapter();
    this.connString = connString;
  }

  public JsonArray mapRed2Array() {
    return mapRed2Array(MapReduceSources.getInstanceForMongoDB());
  }

  public JsonArray mapRed2Array(MapReduceSources mrs) {
    return adapter.stream2JsonArray(performMapReduce(mrs));
  }

  private Stream<JsonObject> performMapReduce(MapReduceSources mrs) {
    MongoClient mongoClient = MongoClients.create(connString);

    // parse the db name from url, i.e. mongodb://localhost:27017/mydbname
    // otherwise use the first database name.
    final var parts = connString.split("/");
    var dbName = mongoClient.listDatabaseNames().first();
    if (parts.length > 0) {
      dbName = parts[parts.length - 1];
    }

    MongoDatabase database = mongoClient.getDatabase(dbName);
    final Map<String, MapReduceIterable<Document>> mapRedMap = new HashMap<>();
    for (String collName : database.listCollectionNames()) {
      var col = database.getCollection(collName);
      mapRedMap.put(collName, col.mapReduce(mrs.getMapJSCode(), mrs.getReduceJSCode()));
    }
    return adapter.adaptStream(mapRedMap);
  }
}
