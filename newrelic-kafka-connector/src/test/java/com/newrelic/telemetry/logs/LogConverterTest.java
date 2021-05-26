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
        expected.put("partitionName", "myPartition");
        expected.put("logLevel", "DEBUG");

        assertEquals("this is the log message", createdLog.getMessage());
        assertEquals("DEBUG", createdLog.getLevel());
        // assertEquals(1621466257L, createdLog.getTimestamp());

        Attributes attributes = createdLog.getAttributes();
        assertEquals(expected, attributes.asMap());

    }

    @Test
    public void withSchema() {
        Log testLog = LogConverter.toNewRelicLog(this.fixtures.sampleStructRecord);
        testEquals(testLog);
    }

    // @Test
    // public void withoutSchema() {
    //     Log testLog = LogConverter.toNewRelicLog(this.fixtures.sampleSchemalessRecord);
    //     testEquals(testLog);
    // }
}