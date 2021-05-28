package com.newrelic.telemetry.logs;

import com.newrelic.telemetry.Attributes;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LogConverterTest {

    Fixtures fixtures;

    @Before
    public void setUp() throws Exception {
        this.fixtures = new Fixtures();
    }

    void testEquals(Log createdLog) {
        Map<String, Object> expected = new HashMap<>();

        expected.put("metadata.kafkaTopic", "myTopic");
        expected.put("metadata.kafkaPartition", "0");
        expected.put("metadata.kafkaOffset", 1001L);
        expected.put("aStringAttribute", "a string attribute");
        expected.put("anIntAttribute", 123);

        // expected.put("message", "{\"service-name\": \"login-service\", \"user\": {\"id\": 123, \"name\": \"alice\"}}");
        assertEquals("{\"service-name\": \"login-service\", \"user\": {\"id\": 123, \"name\": \"alice\"}}", createdLog.getMessage());

        Attributes attributes = createdLog.getAttributes();
        assertEquals(expected, attributes.asMap());

    }

    @Test
    public void withSchema() {
        Log testLog = LogConverter.toNewRelicLog(this.fixtures.sampleStructRecord);
        testEquals(testLog);
    }

    @Test
    public void withoutSchema() {
        Log testLog = LogConverter.toNewRelicLog(this.fixtures.sampleSchemalessRecord);
        testEquals(testLog);
    }

}