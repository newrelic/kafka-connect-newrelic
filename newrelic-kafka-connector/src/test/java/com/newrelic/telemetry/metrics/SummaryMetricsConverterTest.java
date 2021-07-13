package com.newrelic.telemetry.metrics;

import com.newrelic.telemetry.Attributes;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SummaryMetricsConverterTest {

    SummaryFixtures fixtures;

    @Before
    public void setUp() throws Exception {
        this.fixtures = new SummaryFixtures();
    }

    void testEquals(Summary createdSummary) {
        Map<String, Object> expected = new HashMap<>();

        // main properties
        assertEquals("prometheus_tsdb_wal_fsync_duration_seconds", createdSummary.getName());
        assertEquals(5, createdSummary.getCount());
        assertEquals(0.009435792, createdSummary.getSum(),0.0);
        assertEquals(0.01, createdSummary.getMin(), 0.0);
        assertEquals(10.01, createdSummary.getMax(), 0.0);
 
        // attributes
        expected.put("instrumentation.metadata.kafka.topic", "myTopic");
        expected.put("instrumentation.metadata.kafka.partition", "0");
        expected.put("instrumentation.metadata.kafka.offset", 1001L);
        expected.put("tags.code", "2xx");
        Map<String, Object> attributes = createdSummary.getAttributes();
        assertEquals(expected, attributes);

        }

    @Test
    public void withSchema() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleStructRecord);
        if (testMetric instanceof Summary) {
            Summary s = (Summary)testMetric;
            testEquals(s);
            assertTrue(System.currentTimeMillis() - s.getStartTimeMs() < 100);
        } else assertTrue(false);
    }

    @Test
    public void withSchemaAndTimestamp() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleStructWithTimestampRecord);
        if (testMetric instanceof Summary) {
            Summary s = (Summary)testMetric;
            testEquals(s);
            assertEquals(50000L, s.getStartTimeMs());
        } else assertTrue(false);
    }

    @Test
    public void withoutSchema() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleSchemalessRecord);
        if (testMetric instanceof Summary) {
            Summary s = (Summary)testMetric;
            testEquals(s);
            assertTrue(System.currentTimeMillis() - s.getStartTimeMs() < 100);
        } else assertTrue(false);
    }

    @Test
    public void withoutSchemaAndTimestamp() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleSchemalessWithTimestampRecord);
        if (testMetric instanceof Summary) {
            Summary s = (Summary)testMetric;
            testEquals(s);
            assertEquals(50000L, s.getStartTimeMs());
        } else assertTrue(false);
    }
}