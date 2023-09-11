package es.um.nosql.s13e.nosqlimport.db.couchdb;

import java.util.List;
import java.util.stream.Stream;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.lightcouch.DesignDocument.MapReduce;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.fernunihagen.IDatabaseImport;
import es.um.nosql.s13e.nosqlimport.util.CouchDBStreamAdapter;
import es.um.nosql.s13e.nosqlimport.util.MapReduceSources;

public class CouchDBImport implements IDatabaseImport {
  private final CouchDBStreamAdapter adapter;
  private final CouchDbProperties connectionProps;

  /**
   * Creates a CouchDB Import instance
   * 
   * @param host     for example localhost, 127.0.0.1 or myurl.de
   * @param port     normally '5984'
   * @param dbName
   * @param username
   * @param password
   */
  public CouchDBImport(CouchDbProperties connectionProps) {
    this.adapter = new CouchDBStreamAdapter();
    this.connectionProps = connectionProps;
  }

  public JsonArray mapRed2Array() {
    return mapRed2Array(MapReduceSources.getInstanceForCouchDB());
  }

  public JsonArray mapRed2Array(MapReduceSources mrs) {
    return adapter.stream2JsonArray(performMapReduce(mrs));
  }

  private Stream<JsonObject> performMapReduce(MapReduceSources mrs) {
    CouchDbClient dbClient = new CouchDbClient(connectionProps);
    MapReduce mapRedObj = new MapReduce();
    mapRedObj.setMap(mrs.getMapJSCode());
    mapRedObj.setReduce(mrs.getReduceJSCode());
    List<JsonObject> list = dbClient.view("_temp_view").tempView(mapRedObj).group(true)
        .includeDocs(false).reduce(true).query(JsonObject.class);

    // This step will apply some custom rules to the elements...
    return adapter.adaptStream(list);
  }
}
