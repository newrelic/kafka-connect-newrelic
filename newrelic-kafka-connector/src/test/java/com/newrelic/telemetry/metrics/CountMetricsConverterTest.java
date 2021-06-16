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

public class CountMetricsConverterTest {

    CountFixtures fixtures;

    @Before
    public void setUp() throws Exception {
        this.fixtures = new CountFixtures();
    }

    void testEquals(Count createdCount) {
        Map<String, Object> expected = new HashMap<>();

        // main properties
        assertEquals("promhttp_metric_handler_requests_total", createdCount.getName());
        assertEquals(1.0, createdCount.getValue(), 1304.0);

        // attributes
        expected.put("metadata.kafkaTopic", "myTopic");
        expected.put("metadata.kafkaPartition", "0");
        expected.put("metadata.kafkaOffset", 1001L);
        expected.put("code","500");
        expected.put("something","else");
        Map<String, Object> attributes = createdCount.getAttributes();
        assertEquals(expected, attributes);

        }

    @Test
    public void withSchema() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleStructRecord);
        if (testMetric instanceof Count) {
            Count c = (Count)testMetric;
            testEquals(c);
            assertTrue(System.currentTimeMillis() - c.getStartTimeMs() < 100);
        } else assertTrue(false);
    }

    @Test
    public void withSchemaAndTimestamp() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleStructWithTimestampRecord);
        if (testMetric instanceof Count) {
            Count c = (Count)testMetric;
            testEquals(c);
            assertEquals(50000L, c.getStartTimeMs());
        } else assertTrue(false);
    }

    @Test
    public void withoutSchema() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleSchemalessRecord);
        if (testMetric instanceof Count) {
            Count c = (Count)testMetric;
            testEquals(c);
            assertTrue(System.currentTimeMillis() - c.getStartTimeMs() < 100);
        } else assertTrue(false);
    }

    @Test
    public void withoutSchemaAndTimestamp() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleSchemalessWithTimestampRecord);
        if (testMetric instanceof Count) {
            Count c = (Count)testMetric;
            testEquals(c);
            assertEquals(50000L, c.getStartTimeMs());
        } else assertTrue(false);
    }
}