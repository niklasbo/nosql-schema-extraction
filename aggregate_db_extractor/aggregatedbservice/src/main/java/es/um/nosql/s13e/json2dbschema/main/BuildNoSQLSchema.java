package es.um.nosql.s13e.json2dbschema.main;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.node.ArrayNode;

import com.google.gson.JsonArray;

import es.um.nosql.s13e.NoSQLSchema.NoSQLSchemaPackage;
import es.um.nosql.s13e.NoSQLSchema.NoSQLSchema;
import es.um.nosql.s13e.json2dbschema.main.util.JSON2Schema;
import es.um.nosql.s13e.json2dbschema.util.abstractjson.IAJAdapter;
import es.um.nosql.s13e.json2dbschema.util.abstractjson.IAJArray;
import es.um.nosql.s13e.json2dbschema.util.abstractjson.impl.gson.GsonAdapter;
import es.um.nosql.s13e.json2dbschema.util.abstractjson.impl.gson.GsonArray;
import es.um.nosql.s13e.util.NoSQLSchemaWriter;

/**
 * @author dsevilla
 *
 */
public class BuildNoSQLSchema {
  public NoSQLSchema buildFromGsonArray(String schemaName, JsonArray jArray) {
    return buildFromArray(schemaName, new GsonArray(jArray), new GsonAdapter());
  }

  private NoSQLSchema buildFromArray(String schemaName, IAJArray objRows, IAJAdapter<?> adapter) {
    return new JSON2Schema<>(NoSQLSchemaPackage.eINSTANCE.getNoSQLSchemaFactory(), adapter)
        .fromJSONArray(schemaName, objRows);
  }

  public void schema2File(NoSQLSchema schema, String outputFile) {
    NoSQLSchemaWriter writer = new NoSQLSchemaWriter();
    writer.write(schema, outputFile);
  }
}
