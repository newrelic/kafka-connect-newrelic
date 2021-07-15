package com.newrelic.telemetry.logs;

import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.HashMap;
import java.util.Map;

public class Fixtures {

    SinkRecord sampleStructRecord;
    SinkRecord sampleStructWithTimestampRecord;
    SinkRecord sampleSchemalessRecord;
    SinkRecord sampleSchemalessWithTimestampRecord;
    Schema schema;
    Schema schemaWithTimestamp;
    Struct recordStructValue;
    Struct recordStructWithTimestampValue;
    Map<String, Object> recordMapValue;
    Map<String, Object> recordMapWithTimestampValue;

    public Fixtures() {

        // CASE: message is embedded JSON

        schema = SchemaBuilder.struct()
                .field("message", Schema.STRING_SCHEMA)
                .field("aStringAttribute", Schema.STRING_SCHEMA)
                .field("anIntAttribute", Schema.INT32_SCHEMA)
                .build();

        schemaWithTimestamp = SchemaBuilder.struct()
                .field("message", Schema.STRING_SCHEMA)
                .field("aStringAttribute", Schema.STRING_SCHEMA)
                .field("anIntAttribute", Schema.INT32_SCHEMA)
                .field("timestamp", Schema.INT64_SCHEMA)
                .build();

        recordMapValue = new HashMap<>();
        recordMapValue.put("message", "{\"service-name\": \"login-service\", \"user\": {\"id\": 123, \"name\": \"alice\"}}");
        recordMapValue.put("aStringAttribute", "a string attribute");
        recordMapValue.put("anIntAttribute", 123);

        recordMapWithTimestampValue = new HashMap<>();
        recordMapWithTimestampValue.putAll(recordMapValue);
        recordMapWithTimestampValue.put("timestamp", 50000L);

        recordStructValue = new Struct(schema);
        recordStructWithTimestampValue = new Struct(schemaWithTimestamp);

        // set the struct to all the same values as the map
        recordMapValue.entrySet().forEach(e -> recordStructValue.put(e.getKey(), e.getValue()));
        recordMapWithTimestampValue.entrySet().forEach(e -> recordStructWithTimestampValue.put(e.getKey(), e.getValue()));

        //create a test record using a schema
        sampleStructRecord = new SinkRecord("myTopic", 0, null, null, schema, recordStructValue, 1001, 1620000000L, TimestampType.NO_TIMESTAMP_TYPE);
        sampleStructWithTimestampRecord = new SinkRecord("myTopic", 0, null, null, schemaWithTimestamp, recordStructWithTimestampValue, 1001, 1620000000L, TimestampType.NO_TIMESTAMP_TYPE);


        // create a test record without a schema
        sampleSchemalessRecord = new SinkRecord("myTopic", 0, null, null, null, recordMapValue, 1001, 1620000000L, TimestampType.NO_TIMESTAMP_TYPE);
        sampleSchemalessWithTimestampRecord = new SinkRecord("myTopic", 0, null, null, null, recordMapWithTimestampValue, 1001, 1620000000L, TimestampType.NO_TIMESTAMP_TYPE);

    }
}
