package com.newrelic.telemetry.events;

import okio.Sink;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.HashMap;
import java.util.Map;

public class Fixtures {

    public SinkRecord sampleStructRecord;

    public SinkRecord sampleStructWithTimestampRecord;

    public SinkRecord sampleSchemalessRecord;

    public SinkRecord sampleSchemalessWithTimestampRecord;

    public Schema schema;

    public Schema schemaWithTimestamp;

    public Struct recordStructValue;

    public Struct recordStructWithTimestampValue;

    public Map<String, Object> recordMapValue;

    public Map<String, Object> recordMapWithTimestampValue;

    public Fixtures() {
        SchemaBuilder builder = SchemaBuilder.struct()
                .field("eventType", Schema.STRING_SCHEMA)
                .field("stringField", Schema.STRING_SCHEMA)
                .field("intField", Schema.INT32_SCHEMA)
                .field("floatField", Schema.FLOAT32_SCHEMA)
                .field("flattened.field.name", Schema.STRING_SCHEMA);
        schema = builder.build();
        schemaWithTimestamp = builder.field("timestamp", Schema.INT64_SCHEMA).build();

        recordMapValue = new HashMap<>();
        recordMapValue.put("eventType", "myTestEvent");
        recordMapValue.put("stringField", "stringValue");
        recordMapValue.put("intField", 10);
        recordMapValue.put("floatField", 9.9f);
        recordMapValue.put("flattened.field.name", "someStringValue");

        recordMapWithTimestampValue = new HashMap<>();
        recordMapWithTimestampValue.putAll(recordMapValue);
        recordMapWithTimestampValue.put("timestamp", 50000L);

        recordStructValue = new Struct(schema);
        // set the struct to all the same values as the map
        recordMapValue.entrySet().forEach(e -> recordStructValue.put(e.getKey(), e.getValue()));

        recordStructWithTimestampValue = new Struct(schemaWithTimestamp);
        recordMapWithTimestampValue.entrySet().forEach(e -> recordStructWithTimestampValue.put(e.getKey(), e.getValue()));

        //create a test record using a schema
        sampleStructRecord = new SinkRecord("myTopic", 0, null, null, schema, recordStructValue, 1001, 1622000000000L, TimestampType.NO_TIMESTAMP_TYPE);

        //create a test record using a schema with a timestamp;
        sampleStructWithTimestampRecord = new SinkRecord("myTopic", 0, null, null, schemaWithTimestamp, recordStructWithTimestampValue, 1001, 1622000000000L, TimestampType.NO_TIMESTAMP_TYPE);

        //create a test record without a schema
        sampleSchemalessRecord = new SinkRecord("myTopic", 0, null, null, null, recordMapValue, 1001, 1622000000000L, TimestampType.NO_TIMESTAMP_TYPE);

        //create a test record without a schema, but with a timestamp
        sampleSchemalessWithTimestampRecord = new SinkRecord("myTopic", 0, null, null, null, recordMapWithTimestampValue, 1001, 1622000000000L, TimestampType.NO_TIMESTAMP_TYPE);
    }
}
