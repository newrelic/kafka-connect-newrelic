package com.newrelic.telemetry.events;

import com.newrelic.telemetry.TelemetrySinkConnectorConfig;
import org.apache.kafka.common.config.ConfigException;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TelemetryEventsSinkConnectorTest {
    private final TelemetryEventsSinkConnector sinkConnector = new TelemetryEventsSinkConnector();

    @Test
    public void testVersion() {
        assertTrue(sinkConnector.version().length() > 0);
    }

    @Test
    public void testTaskClass() {
        assertEquals(TelemetryEventsSinkTask.class, sinkConnector.taskClass());
    }

    @Test
    public void testTaskConfigs() {
        Map<String, String> configProps = Collections.singletonMap("key1", "value1");
        sinkConnector.start(configProps);

        List<Map<String, String>> taskConfigs = sinkConnector.taskConfigs(7);
        assertEquals(7, taskConfigs.size());
        for(Map<String, String> config: taskConfigs) {
            assertEquals(configProps, config);
        }
    }

    @Test
    public void testConfig() {
        assertEquals(TelemetrySinkConnectorConfig.conf(), sinkConnector.config());
    }

    @Test
    public void testConfigWithApiKey_ShouldWork() {
        Map<String, String> props = Collections.singletonMap("api.key", "abcdefghijklmnopqrstuvwxyz");
        sinkConnector.validate(props);
    }
}