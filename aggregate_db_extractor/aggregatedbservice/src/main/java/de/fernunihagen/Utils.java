package de.fernunihagen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lightcouch.CouchDbProperties;

import de.fernunihagen.exceptions.EntityHasNoPropertiesException;

public class Utils {

    private Utils() {
    }

    public static String getResultsDirForJobId(final String jobId) {
        return "." + File.separator + "results" + File.separator + jobId;
    }

    public static String getXmlSchemaFileForJobId(final String jobId) {
        return getResultsDirForJobId(jobId) + File.separator + "schema.xml";
    }

    public static String getJsonSchemaFileForJobId(final String jobId) {
        return getResultsDirForJobId(jobId) + File.separator + "schema.json";
    }

    /**
     * parses a connection string for couch db
     * 
     * @param connString couchdb://localhost[:1234]/dbname
     * @param username
     * @param password
     * @return
     */
    public static CouchDbProperties parseCouchDBConnectionString(final String connString, final String username,
            final String password) {
        final var withoutPrefix = connString.trim().substring("couchdb://".length());
        var dbHost = "";
        if (withoutPrefix.contains(":")) {
            dbHost = withoutPrefix.split(":")[0];
        } else {
            // port is missing
            dbHost = withoutPrefix.split("/")[0];
        }
        final var withoutHost = withoutPrefix.substring(dbHost.length());
        var dbPort = 5984;
        if (withoutHost.contains(":") && withoutHost.contains("/")) {
            dbPort = Integer.parseInt(withoutHost.substring(1, withoutHost.indexOf("/")));
            // beginIndex=1 because of the ':' after the hostname
        }
        var dbName = "";
        final var parts = withoutPrefix.split("/");
        if (parts.length > 0) {
            dbName = parts[parts.length - 1];
        }

        return new CouchDbProperties(dbName, true, "http", dbHost, dbPort, username, password);
    }

    /**
     * Converts XML-NoSQLSchema style to JSON Schema
     * (https://json-schema.org/draft/2020-12/schema)
     * 
     * Further it merges the schema variations to a union set of types.
     * 
     * @param xmlData parsed NoSQLSchema XML file
     * @return List of json schema Maps (version draft 2020-12)
     */
    public static List<Map<String, Object>> xmlSchemaToJsonSchema(final Map<String, Object> xmlData) {
        final var allJsonSchema = new ArrayList<Map<String, Object>>();

        final var rawEntities = xmlData.get("entities");

        if (LinkedHashMap.class.equals(rawEntities.getClass())) {
            System.out.println("xml schema is a map - so only one entity to convert");
            final var entity = (Map<String, Object>) rawEntities;
            final var allEntitySchemaIds = new ArrayList<String>();
            allEntitySchemaIds.add(xmlData.get("name") + "." + entity.get("name") + ".schema.json");
            try {
                allJsonSchema.add(convertEntity(entity, allEntitySchemaIds.get(0), allEntitySchemaIds));
            } catch (EntityHasNoPropertiesException e) {
                System.out.println("WARNING: " + e.getMessage() + ". XML Object in Java Notation: " + entity);
            }
        }
        if (ArrayList.class.equals(rawEntities.getClass())) {
            System.out.println("xml schema is a list - so multiple entities to convert");

            final var allEntitySchemaIds = new ArrayList<String>();
            // read Entity names for references
            for (Object e : (List) rawEntities) {
                final var entity = (Map<String, Object>) e;
                allEntitySchemaIds.add(xmlData.get("name") + "." + entity.get("name") + ".schema.json");
            }
            var index = 0;
            for (Object rawEntity : (List) rawEntities) {
                final var entity = (Map<String, Object>) rawEntity;
                try {
                    allJsonSchema.add(convertEntity(entity, allEntitySchemaIds.get(index), allEntitySchemaIds));
                } catch (EntityHasNoPropertiesException e) {
                    System.out.println("WARNING: " + e.getMessage() + ". XML Object in Java Notation: " + entity);
                }
                index++;
            }
        }
        return allJsonSchema;
    }

    private static Map<String, Object> convertEntity(final Map<String, Object> entity, final String entityId,
            final List<String> allEntitySchemaIds) throws EntityHasNoPropertiesException {
        final var jsonSchema = new HashMap<String, Object>();
        jsonSchema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        jsonSchema.put("$id", entityId);
        jsonSchema.put("title", entity.get("name"));
        jsonSchema.put("description",
                "Extracted and transformed schema for entity " + entity.get("name"));
        jsonSchema.put("type", "object");

        final var rawVariations = entity.get("variations");
        if (LinkedHashMap.class.equals(rawVariations.getClass())) {
            System.out.println(entity.get("name") + "'s variations is a map - only one variation to convert");
            final var variations = (Map<String, Object>) rawVariations;
            final var properties = (List) variations.get("properties");
            if (properties == null) {
                throw new EntityHasNoPropertiesException(
                        "entity has no properties and will be skipped: " + entity.get("name"));
            }
            final var jsonSchemaProps = new HashMap<String, Object>();
            final var requiredProps = new ArrayList<String>();
            for (final Object o : properties) {
                final var simpleProp = simplifyProperties((Map<String, Object>) o, allEntitySchemaIds);
                jsonSchemaProps.put((String) simpleProp.keySet().toArray()[0],
                        simpleProp.get(simpleProp.keySet().toArray()[0]));
                requiredProps.add((String) simpleProp.keySet().toArray()[0]);
            }
            jsonSchema.put("properties", jsonSchemaProps);
            jsonSchema.put("required", requiredProps);
        } else if (ArrayList.class.equals(rawVariations.getClass())) {
            System.out.println(entity.get("name") + "'s variations is a list - multiple variations to convert & merge");
            final var variations = (List<Map<String, Object>>) rawVariations;

            final var jsonSchemaProps = new HashMap<String, Object>();
            final var requiredProps = new ArrayList<String>();

            for (Map<String, Object> variation : variations) {
                final var properties = (List) variation.get("properties");
                if (properties == null) {
                    System.out.println("WARNING: entity has no properties and will be skipped: " + entity.get("name"));
                    continue;
                }
                for (final Object o : properties) {
                    final var simpleProp = simplifyProperties((Map<String, Object>) o, allEntitySchemaIds);
                    final var propKey = (String) simpleProp.keySet().toArray()[0];
                    if (jsonSchemaProps.containsKey(propKey)) {
                        // we have to merge the data types
                        final var original = jsonSchemaProps.get(propKey);
                        final var next = simpleProp.get(propKey);
                        if (!original.equals(next)) {
                            final var merged = mergePropTypes((Map<String, Object>) original,
                                    (Map<String, Object>) next);
                            jsonSchemaProps.put(propKey, merged);
                        }
                    } else {
                        jsonSchemaProps.put(propKey,
                                simpleProp.get(simpleProp.keySet().toArray()[0]));
                        requiredProps.add(propKey);
                    }
                }
            }
            jsonSchema.put("properties", jsonSchemaProps);
        }
        return jsonSchema;
    }

    /**
     * Merges property types
     * 
     * @param original {"type": "String"} or {"type": ["String", "Number"]}
     * @param next     {"type": "Null"}
     * @return a merged prop type, {"type": ["String", "Null"]} or {"type":
     *         ["String", "Number", "Null"]}
     */
    private static Map<String, Object> mergePropTypes(Map<String, Object> original, Map<String, Object> next) {
        final var merged = new HashMap<String, Object>();
        final var orginalTypeRaw = original.get("type");
        if (ArrayList.class.equals(orginalTypeRaw.getClass())) {
            final var originalType = (ArrayList) orginalTypeRaw;
            originalType.add(next.get("type"));
            merged.put("type", originalType);
        } else {
            final var originalType = (String) orginalTypeRaw;
            final var listOfMergedTypes = new ArrayList<Object>();
            listOfMergedTypes.add(originalType);
            listOfMergedTypes.add(next.get("type"));
            merged.put("type", listOfMergedTypes);
        }
        return merged;
    }

    /**
     * Input:
     * 
     * @param in        is the parsed JSON from the XML (NoSQLSchema)
     * 
     *                  <pre>
    {
    "type": [
        "NoSQLSchema:Attribute",
        {
        "type": "NoSQLSchema:PrimitiveType",
        "name": "String"
        }
    ],
    "name": "city"
    }
     *                  </pre>
     * 
     * @param entityIds is the list of entity names, only needed for references
     * 
     *                  <pre>
     * [ "jobid.EntityNameA.schema.json", "jobid.EntityNameB.schema.json"]
     *                  </pre>
     * 
     */
    public static Map<String, Object> simplifyProperties(final Map<String, Object> in, final List<String> entityIds) {
        final var name = (String) in.get("name");
        final var out = new HashMap<String, Object>();

        final var outProps = new HashMap<String, Object>();

        final var rawType = in.get("type");
        if (ArrayList.class.equals(rawType.getClass())) {
            // type NoSQLSchema:Attribute
            final var rawTypeList = (List) rawType;
            if (rawTypeList.size() != 2) {
                System.out.println("WARNING: property '" + name + "'' has not 2 elements");
            }
            for (Object object : rawTypeList) {
                if (LinkedHashMap.class.equals(object.getClass())) {
                    final var t = (Map) object;
                    if ("NoSQLSchema:PList".equals(t.get("type"))) {
                        final var rawElementType = (Map) t.get("elementType");

                        final var itemMap = new HashMap<String, Object>();
                        itemMap.put("type", ((String) rawElementType.get("name")).toLowerCase());
                        outProps.put("type", "array");
                        outProps.put("items", itemMap);
                    } else if ("NoSQLSchema:PrimitiveType".equals(t.get("type"))) {
                        outProps.put("type", ((String) t.get("name")).toLowerCase());
                    } else {
                        throw new RuntimeException("Unknown type: " + t.get("type") + " in: " + name);
                    }
                }
            }
        } else {
            // type NoSQLSchema:Aggregate or NoSQLSchema:Reference
            final var rawTypeString = (String) rawType;
            if ("NoSQLSchema:Aggregate".equals(rawTypeString)) {
                outProps.put("description", "nested entity");
                outProps.put("$ref", getEntityReference((String) in.get("aggregates"), entityIds));
            } else if ("NoSQLSchema:Reference".equals(rawTypeString)) {
                outProps.put("description", "reference to " + getEntityReference((String) in.get("refsTo"), entityIds));
                outProps.put("type", ((String) in.get("originalType")).toLowerCase());
            } else {
                throw new RuntimeException("Unknown type: " + rawTypeString + " in: " + name);
            }
        }

        out.put(name, outProps);
        return out;
    }

    private static final String XML_ENTITY_PREFIX = "//@entities.";
    private static final String XML_VARIATION_INFIX = "/@variations.";

    private static String getEntityReference(String xmlRef, List<String> entityIds) {
        if (xmlRef.startsWith(XML_ENTITY_PREFIX)) {
            final var trimmed = xmlRef.substring(XML_ENTITY_PREFIX.length());
            if (trimmed.contains(XML_VARIATION_INFIX)) {
                // ref type is: //@entities.6/@variations.0
                return entityIds.get(Integer.parseInt(trimmed.split("/")[0]));
            } else {
                // ref type is: //@entities.6
                return entityIds.get(Integer.parseInt(trimmed));
            }
        }
        throw new RuntimeException("unknown format for a xml reference: " + xmlRef);
    }
}
