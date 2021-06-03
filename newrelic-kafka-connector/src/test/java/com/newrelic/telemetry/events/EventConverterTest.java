package com.newrelic.telemetry.events;

import com.newrelic.telemetry.Attributes;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventConverterTest {

    Fixtures fixtures;

    @Before
    public void setUp() throws Exception {
        this.fixtures = new Fixtures();
    }

    void testEquals(Event createdEvent) {
        Map<String, Object> expected = new HashMap<>();

        expected.put("instrumentation.metadata.kafka.topic", "myTopic");
        expected.put("instrumentation.metadata.kafka.partition", "0");
        expected.put("instrumentation.metadata.kafka.offset", 1001L);
        expected.put("stringField", "stringValue");
        expected.put("intField", 10);
        expected.put("floatField", 9.9f);
        expected.put("flattened.field.name", "someStringValue");

        assertEquals("myTestEvent", createdEvent.getEventType());

        Attributes attributes = createdEvent.getAttributes();
        assertEquals(expected, attributes.asMap());


    }

    @Test
    public void withSchema() {
        Event testEvent = EventConverter.toNewRelicEvent(this.fixtures.sampleStructRecord);
        testEquals(testEvent);
        assertTrue(System.currentTimeMillis() - testEvent.getTimestamp() < 100);
    }

    @Test
    public void withSchemaAndTimestamp() {
        Event testEvent = EventConverter.toNewRelicEvent(this.fixtures.sampleStructWithTimestampRecord);
        testEquals(testEvent);
        assertEquals(50000L, testEvent.getTimestamp());
    }

    @Test
    public void withoutSchema() {
        Event testEvent = EventConverter.toNewRelicEvent(this.fixtures.sampleSchemalessRecord);
        testEquals(testEvent);
        assertTrue(System.currentTimeMillis() - testEvent.getTimestamp() < 100);
    }

    @Test
    public void withoutSchemaAndTimestamp() {
        Event testEvent = EventConverter.toNewRelicEvent(this.fixtures.sampleSchemalessWithTimestampRecord);
        testEquals(testEvent);
        assertEquals(50000L, testEvent.getTimestamp());
    }
}