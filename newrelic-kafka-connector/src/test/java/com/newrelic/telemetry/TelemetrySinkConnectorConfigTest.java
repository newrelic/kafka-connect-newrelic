package com.newrelic.telemetry;

import org.junit.Test;
import static org.junit.Assert.*;

public class TelemetrySinkConnectorConfigTest {

    @Test
    public void doc() {
        String stringConf = TelemetrySinkConnectorConfig.conf().toRst();
        assertTrue(stringConf.contains("api.key"));
        assertTrue(stringConf.contains("max.retries"));
        assertTrue(stringConf.contains("retry.interval.ms"));
    }
}
