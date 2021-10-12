package com.newrelic.telemetry.metrics;

import okio.Sink;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.HashMap;
import java.util.Map;

public class CountFixtures {

    public SinkRecord sampleStructRecord;
    public SinkRecord sampleSchemalessRecord;
    public SinkRecord sampleStructWithTimestampRecord;
    public SinkRecord sampleSchemalessWithTimestampRecord;

    public Schema schema;
    public Schema schemaWithTimestamp;

    public Struct recordStructValue;
    public Struct recordStructWithTimestampValue;

    public Map<String, Object> recordMapValue;
    public Map<String, Object> recordMapWithTimestampValue;


    public CountFixtures() {

        Schema DIMENSIONS_MAP_SCHEMA = SchemaBuilder.map(
            Schema.STRING_SCHEMA,
            Schema.STRING_SCHEMA
        )
        .name("dimensionSchema")
        .build();

        SchemaBuilder builder = SchemaBuilder.struct()
                .field("name", Schema.STRING_SCHEMA)
                .field("type", Schema.STRING_SCHEMA)
                .field("value", Schema.FLOAT64_SCHEMA)
                .field("dimensions", DIMENSIONS_MAP_SCHEMA);
        schema = builder.build();
        schemaWithTimestamp = builder.field("timestamp", Schema.INT64_SCHEMA).build();

        Map<String, String> dimensionsMap = new HashMap<String, String>();
        dimensionsMap.put("code","500");
        dimensionsMap.put("something","else");

        recordMapValue = new HashMap<>();
        recordMapValue.put("name", "promhttp_metric_handler_requests_total");
        recordMapValue.put("type", "counter");
        recordMapValue.put("value", 1304.0);
        recordMapValue.put("dimensions", dimensionsMap);

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
