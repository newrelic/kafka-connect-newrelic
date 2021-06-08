package com.newrelic.telemetry.metrics;

import okio.Sink;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.HashMap;
import java.util.Map;

public class SummaryFixtures {

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


    public SummaryFixtures() {

        SchemaBuilder builder = SchemaBuilder.struct()
                .field("name", Schema.STRING_SCHEMA)
                .field("aggregated_summary.count", Schema.INT32_SCHEMA)
                .field("aggregated_summary.sum", Schema.FLOAT64_SCHEMA)
                .field("aggregated_summary.min", Schema.FLOAT64_SCHEMA)
                .field("aggregated_summary.max", Schema.FLOAT64_SCHEMA)
                .field("tags.code", Schema.STRING_SCHEMA);
                // .field("aggregated_summary.quantiles.upper_limit", Schema.FLOAT64_SCHEMA)
                // .field("aggregated_summary.quantiles.upper_limit", Schema.FLOAT64_SCHEMA)
        schema = builder.build();
        schemaWithTimestamp = builder.field("timestamp", Schema.INT64_SCHEMA).build();

        recordMapValue = new HashMap<>();
        recordMapValue.put("name", "prometheus_tsdb_wal_fsync_duration_seconds");
        recordMapValue.put("aggregated_summary.count", 5);
        recordMapValue.put("aggregated_summary.sum", 0.009435792);
        recordMapValue.put("aggregated_summary.min", 0.01);
        recordMapValue.put("aggregated_summary.max", 10.01);
        recordMapValue.put("tags.code", "2xx");

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
