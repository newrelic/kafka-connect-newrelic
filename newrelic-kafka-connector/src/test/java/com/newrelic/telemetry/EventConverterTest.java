package com.newrelic.telemetry;

import com.newrelic.telemetry.events.EventConverter;
import com.newrelic.telemetry.events.models.EventModel;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EventConverterTest {



    SinkRecord sampleStructRecord;

    SinkRecord sampleSchemalessRecord;

    Schema schema;

    Struct recordStructValue;

    Map<String, Object> recordMapValue;

    @Before
    public void setUp() throws Exception {

        schema = SchemaBuilder.struct()
                .field("eventType", Schema.STRING_SCHEMA)
                .field("stringField", Schema.STRING_SCHEMA)
                .field("intField", Schema.INT32_SCHEMA)
                .field("floatField", Schema.FLOAT32_SCHEMA)
                .field("flattened.field.name", Schema.STRING_SCHEMA)
                .build();

        recordMapValue = new HashMap<>();
        recordMapValue.put("eventType", "myTestEvent");
        recordMapValue.put("stringField", "stringValue");
        recordMapValue.put("intField", 10);
        recordMapValue.put("floatField", 9.9f);
        recordMapValue.put("flattened.field.name", "someStringValue");

        recordStructValue = new Struct(schema);

        // set the struct to all the same values as the map
        recordMapValue.entrySet().forEach(e -> recordStructValue.put(e.getKey(), e.getValue()));

        //create a test record using a schema
        sampleStructRecord = new SinkRecord("myTopic", 0, null, null, schema, recordStructValue, 1001, 1621466257L, TimestampType.NO_TIMESTAMP_TYPE);

        //create a test record without a schema
        sampleSchemalessRecord = new SinkRecord("myTopic", 0, null, null, null, recordMapValue, 1001, 1621466257L, TimestampType.NO_TIMESTAMP_TYPE);
    }

    void testEquals(EventModel createdEvent) {
        assertEquals("myTestEvent", createdEvent.eventType);
        assertEquals(1621466257L, createdEvent.timestamp);
        assertEquals("myTopic", createdEvent.otherFields().get("metadata.kafkaTopic"));
        assertEquals("0", createdEvent.otherFields().get("metadata.kafkaPartition"));
        assertEquals(1001L, createdEvent.otherFields().get("metadata.kafkaOffset"));
        assertEquals("stringValue", createdEvent.otherFields().get("stringField"));
        assertEquals(10, createdEvent.otherFields().get("intField"));
        assertEquals(9.9f, createdEvent.otherFields().get("floatField"));
        assertEquals("someStringValue", createdEvent.otherFields().get("flattened.field.name"));
    }

    @Test
    public void withSchema() {
        EventModel testEvent = EventConverter.withSchema(sampleStructRecord);
        testEquals(testEvent);
    }

    @Test
    public void withoutSchema() {
        EventModel testEvent = EventConverter.withoutSchema(sampleSchemalessRecord);
        testEquals(testEvent);
    }
}