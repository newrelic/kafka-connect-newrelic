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

public class GaugeMetricsConverterTest {

    GaugeFixtures fixtures;

    @Before
    public void setUp() throws Exception {
        this.fixtures = new GaugeFixtures();
    }

    void testEquals(Gauge createdGauge) {
        Map<String, Object> expected = new HashMap<>();

        // main properties
        assertEquals("prometheus_tsdb_wal_segment_current", createdGauge.getName());
        assertEquals(1.0, createdGauge.getValue(), 0.0);

        // attributes
        expected.put("metadata.kafkaTopic", "myTopic");
        expected.put("metadata.kafkaPartition", "0");
        expected.put("metadata.kafkaOffset", 1001L);
        expected.put("tags.code", "500");
        Map<String, Object> attributes = createdGauge.getAttributes();
        assertEquals(expected, attributes);

        }

    @Test
    public void withSchema() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleStructRecord);
        if (testMetric instanceof Gauge) {
            Gauge g = (Gauge)testMetric;
            testEquals(g);
            assertTrue(System.currentTimeMillis() - g.getTimestamp() < 100);
        } else assertTrue(false);
    }

    @Test
    public void withSchemaAndTimestamp() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleStructWithTimestampRecord);
        if (testMetric instanceof Gauge) {
            Gauge g = (Gauge)testMetric;
            testEquals(g);
            assertEquals(50000L, g.getTimestamp());
        } else assertTrue(false);
    }

    @Test
    public void withoutSchema() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleSchemalessRecord);
        if (testMetric instanceof Gauge) {
            Gauge g = (Gauge)testMetric;
            testEquals(g);
            assertTrue(System.currentTimeMillis() - g.getTimestamp() < 100);
        } else assertTrue(false);
    }

    @Test
    public void withoutSchemaAndTimestamp() {
        Metric testMetric = MetricConverter.toNewRelicMetric(this.fixtures.sampleSchemalessWithTimestampRecord);
        if (testMetric instanceof Gauge) {
            Gauge g = (Gauge)testMetric;
            testEquals(g);
            assertEquals(50000L, g.getTimestamp());
        } else assertTrue(false);
    }
}