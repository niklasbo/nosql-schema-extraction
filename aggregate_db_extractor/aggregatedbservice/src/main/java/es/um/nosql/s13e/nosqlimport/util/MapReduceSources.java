package es.um.nosql.s13e.nosqlimport.util;

public class MapReduceSources {
  private static final String mapJSCodeMongoDB = "function map()\n{\n  ///// BEGIN TimestampAnalyzer declaration /////\n  var TimestampAnalyzer =\n  {\n    getAttrValue: function() {return 0;},\n    analyzeAttribute: function(attrName, attrValue) {}\n  };\n  ///// END TimestampAnalyzer declaration /////\n\n  function flatten_schema_str(obj, interesting_keys)\n  {\n    return JSON.stringify(flatten_schema(obj, interesting_keys));\n  }\n\n  function flatten_schema(obj, interesting_keys)\n  {\n    var retschema = null;\n\n    function _search_string_in_array(str, strArray)\n    {\n      return !strArray.every(function (s)\n      {\n        return !s.toLowerCase().match(str);\n      });\n    }\n\n    function _complex_obj(obj, asObject)\n    {\n      var _retschema;\n      if (asObject)\n        _retschema = {};\n      else\n        _retschema = [];\n\n      Object.keys(obj).sort().forEach(function (key)\n      {\n        TimestampAnalyzer.analyzeAttribute(key, obj[key]);\n\n        //if (!obj.hasOwnProperty(key))\n        //    return;\n        if (asObject)\n        {\n          if ((typeof interesting_keys !== \'undefined\')\n            && _search_string_in_array(key, interesting_keys))\n            _retschema[key] = obj[key];\n          else\n            _retschema[key] = flatten_schema(obj[key]);\n        }\n        else\n          _retschema.push(flatten_schema(obj[key]));\n      });\n\n      return _retschema;\n    };\n\n    // Array\n    if (Array.isArray(obj))\n    {\n      if (obj.length == 0)\n        retschema = [];\n      else\n      {\n        // See if we can produce just one array object with one inside type (homogeneous)\n        var schemas = obj.map(function (e) { return flatten_schema(e); });\n\n        var str_schema_0 = JSON.stringify(schemas[0]);\n        if (obj.length == 1 || schemas.every(function (e) { return JSON.stringify(e) == str_schema_0; }))\n          retschema = [ schemas[0] ];\n        else\n          retschema = schemas;\n      }\n    } // null\n    else if (obj === null)\n    {\n      retschema = null;\n    } // Date\n    else if (obj instanceof Date)\n    {\n      retschema = new Date();\n    } // Object\n    else if ((typeof obj) == \'object\')\n    {\n      if (obj instanceof ObjectId)\n        retschema = \"oid\";\n      else if (obj instanceof NumberLong)\n        retschema = 0;\n      else\n        retschema = _complex_obj(obj, true);\n    }\n    // Other\n    else if ((typeof obj) == \'number\')\n    {\n      retschema = 0;\n    } else if ((typeof obj == \'boolean\'))\n    {\n      retschema = true;\n    } else if ((typeof obj == \'string\'))\n    {\n      retschema = \"s\";\n    }\n\n    return retschema;\n  }\n\n  var schema = flatten_schema_str(this, []);\n\n  emit(schema, {schema: schema, count: 1, firstTimestamp: TimestampAnalyzer.getAttrValue(), lastTimestamp: TimestampAnalyzer.getAttrValue()});\n}";
  private static final String reduceJSCodeMongoDB = "function reduce(key, values)\n{\n  var v = values.reduce(function (v1, v2)\n    {\n      return {schema: v2.schema,\n              count: v1.count + v2.count,\n              firstTimestamp: Math.min(v1.firstTimestamp, v2.firstTimestamp),\n              lastTimestamp: Math.max(v1.lastTimestamp, v2.lastTimestamp)\n            };\n    }, {schema: null,\n        count: 0,\n        firstTimestamp: new Date().getTime(),\n        lastTimestamp: 0});\n\n  return v;\n}";

  private static final String mapJSCodeCouchDB = "function(doc) {\n  function flatten_schema_str(obj, interesting_keys) {\n    return JSON.stringify(flatten_schema(obj, interesting_keys));\n  }\n\n  function flatten_schema(obj, interesting_keys) {\n    var retschema = null;\n\n    function _search_string_in_array(str, strArray) {\n      return !strArray.every(function (s) {\n        return !s.toLowerCase().match(str);\n      });\n    }\n\n    function _complex_obj(obj, asObject) {\n      var _retschema;\n      if (asObject)\n        _retschema = {};\n      else\n        _retschema = [];\n\n      Object.keys(obj).sort().forEach(function (key) {\n        //if (!obj.hasOwnProperty(key))\n        //    return;\n        if (asObject) {\n          if ((typeof interesting_keys !== \'undefined\')\n            && _search_string_in_array(key, interesting_keys))\n            _retschema[key] = obj[key];\n          else\n            _retschema[key] = flatten_schema(obj[key]);\n        }\n        else\n          _retschema.push(flatten_schema(obj[key]));\n      });\n      return _retschema;\n    };\n\n    // Array\n    if (isArray(obj)) {\n      if (obj.length == 0)\n        retschema = [];\n      else {\n        // See if we can produce just one array object with one inside type (homogeneous)\n        var schemas = obj.map(function (e) { return flatten_schema(e); });\n\n        var str_schema_0 = JSON.stringify(schemas[0]);\n        if (obj.length == 1 || schemas.every(function (e) { return JSON.stringify(e) == str_schema_0; }))\n          retschema = [schemas[0]];\n        else\n          retschema = schemas;\n      }\n    } // null\n    else if (obj === null) {\n      retschema = null;\n    } // Date\n    else if (obj instanceof Date) {\n      retschema = new Date();\n    }// Object\n    else if ((typeof obj) == \'object\') {\n      retschema = _complex_obj(obj, true);\n    }\n    // Other\n    else if ((typeof obj) == \'number\') {\n      retschema = 0;\n    } else if ((typeof obj == \'boolean\')) {\n      retschema = true;\n    } else if ((typeof obj == \'string\')) {\n      retschema = \'s\';\n    }\n\n    return retschema;\n  }\n\n  var schema = flatten_schema_str(doc, []);\n  emit(schema, { schema: schema, count: 1, firstTimestamp: 0, lastTimestamp: 0 });\n}";
  private static final String reduceJSCodeCouchDB = "function (key, values) {\n  var v = values.reduce(function (v1, v2) {\n    return {\n      schema: v2.schema,\n      count: v1.count + v2.count\n, firstTimestamp: 0, lastTimestamp: 0    };\n  },\n    { schema: null, count: 0, firstTimestamp: 0, lastTimestamp: 0 });\n  return v;\n}";
  private final boolean isMongoDB;

  public static MapReduceSources getInstanceForMongoDB() {
    return new MapReduceSources(true);
  }

  public static MapReduceSources getInstanceForCouchDB() {
    return new MapReduceSources(false);
  }

  private MapReduceSources(final boolean isMongoDB) {
    this.isMongoDB = isMongoDB;
  }

  public String getMapJSCode() {
    if (isMongoDB)
      return mapJSCodeMongoDB;
    return mapJSCodeCouchDB;
  }

  public String getReduceJSCode() {
    if (isMongoDB)
      return reduceJSCodeMongoDB;
    return reduceJSCodeCouchDB;
  }
}
