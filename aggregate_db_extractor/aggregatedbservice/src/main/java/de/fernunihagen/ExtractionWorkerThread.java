package de.fernunihagen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;

import de.fernunihagen.models.ExtractionQueueItem;
import es.um.nosql.s13e.json2dbschema.main.BuildNoSQLSchema;
import es.um.nosql.s13e.nosqlimport.db.couchdb.CouchDBImport;
import es.um.nosql.s13e.nosqlimport.db.mongodb.MongoDBImport;

import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class ExtractionWorkerThread extends Thread {

    private final Queue<ExtractionQueueItem> queue;
    private final IUpdateRequestState parent;

    public ExtractionWorkerThread(final Queue<ExtractionQueueItem> queue, final IUpdateRequestState parent) {
        super("ExtractionWorkerThread");
        this.queue = queue;
        this.parent = parent;
    }

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (final InterruptedException e) {
                e.printStackTrace(); // normal interrupt by user
            }
            final var req = queue.poll();
            if (req != null) {
                parent.updateRequestState(req.getJobId(), Controller.STATE_RUNNING);
                final var outputDirectoryName = Utils.getResultsDirForJobId(req.getJobId());
                final File theDir = new File(outputDirectoryName);
                if (!theDir.exists()) {
                    theDir.mkdirs();
                }
                final var outputFileName = Utils.getXmlSchemaFileForJobId(req.getJobId());
                try {
                    IDatabaseImport dbImportInstance = null;
                    if (req.getUri().startsWith("mongodb")) {
                        dbImportInstance = new MongoDBImport(req.getUri());
                    } else if (req.getUri().startsWith("couchdb")) {
                        final var dbProps = Utils.parseCouchDBConnectionString(req.getUri(), req.getUser(),
                                req.getPassword());
                        dbImportInstance = new CouchDBImport(dbProps);
                    } else {
                        System.out.println(
                                "could not identify the database. database connection string (uri) must start with 'mongodb' or 'couchdb://'");
                        parent.updateRequestState(req.getJobId(), Controller.STATE_ABORTED);
                        continue;
                    }
                    final JsonArray jArray = dbImportInstance.mapRed2Array();
                    System.out.println("Inference finished.");

                    System.out.println("Starting BuildNoSQLSchema...");

                    final BuildNoSQLSchema builder = new BuildNoSQLSchema();
                    final var s = builder.buildFromGsonArray(req.getJobId(), jArray);
                    builder.schema2File(s, outputFileName);

                    System.out.println("Finished BuildNoSQLSchema...");

                    // map xml to json
                    final XmlMapper xmlMapper = new XmlMapper();
                    final Map<String, Object> xmldata = xmlMapper.readValue(new File(outputFileName), Map.class);

                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(new File(Utils.getJsonSchemaFileForJobId(req.getJobId())),
                            Utils.xmlSchemaToJsonSchema(xmldata));

                    // map xml to json end

                    parent.updateRequestState(req.getJobId(), Controller.STATE_FINISHED);
                } catch (final Exception e) {
                    parent.updateRequestState(req.getJobId(), Controller.STATE_ABORTED);
                    e.printStackTrace();
                }
            }
        }
    }
}
