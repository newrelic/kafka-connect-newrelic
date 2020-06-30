package com.newrelic.telemetry;

import org.junit.Test;

public class TelemetrySinkConnectorConfigTest {
  @Test
  public void doc() {

    System.out.println(TelemetrySinkConnectorConfig.conf().toRst());
  }
}
