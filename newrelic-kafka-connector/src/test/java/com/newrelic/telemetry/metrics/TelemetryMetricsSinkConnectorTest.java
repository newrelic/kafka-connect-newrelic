package com.newrelic.telemetry.metrics;

import com.newrelic.telemetry.TelemetrySinkConnectorConfig;
import com.newrelic.telemetry.events.TelemetryEventsSinkConnector;
import com.newrelic.telemetry.events.TelemetryEventsSinkTask;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TelemetryMetricsSinkConnectorTest {
  private final TelemetryMetricsSinkConnector sinkConnector = new TelemetryMetricsSinkConnector();

  @Test
  public void testVersion() {
    assertTrue(sinkConnector.version().length() > 0);
  }

  @Test
  public void testTaskClass() {
    assertEquals(TelemetryMetricsSinkTask.class, sinkConnector.taskClass());
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
}
