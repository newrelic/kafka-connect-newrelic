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

    SinkRecord sampleSchemalessRecord;

    Schema schema;

    Struct recordStructValue;

    Map<String, Object> recordMapValue;

    public Fixtures() {
        schema = SchemaBuilder.struct()
                .field("partitionName", Schema.STRING_SCHEMA)
                .field("logMessage", Schema.STRING_SCHEMA)
                .field("logLevel", Schema.STRING_SCHEMA)
                .build();

        recordMapValue = new HashMap<>();
        recordMapValue.put("partitionName", "myPartition");
        recordMapValue.put("logMessage", "this is the log message");
        recordMapValue.put("logLevel", "DEBUG");

        recordStructValue = new Struct(schema);

        // set the struct to all the same values as the map
        recordMapValue.entrySet().forEach(e -> recordStructValue.put(e.getKey(), e.getValue()));

        //create a test record using a schema
        sampleStructRecord = new SinkRecord("myTopic", 0, null, null, schema, recordStructValue, 1001, 1621466257L, TimestampType.NO_TIMESTAMP_TYPE);

        // create a test record without a schema
        // sampleSchemalessRecord = new SinkRecord("myTopic", 0, null, null, null, recordMapValue, 1001, 1621466257L, TimestampType.NO_TIMESTAMP_TYPE);

    }
}
