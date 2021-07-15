package com.newrelic.telemetry;

import org.junit.Test;
import static org.junit.Assert.*;

public class TelemetrySinkConnectorConfigTest {

    @Test
    public void doc() {
        String stringConf = TelemetrySinkConnectorConfig.conf().toRst();
        assertTrue(stringConf.contains(TelemetrySinkConnectorConfig.API_KEY));
        //assertTrue(stringConf.contains(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS));
        //assertTrue(stringConf.contains(TelemetrySinkConnectorConfig.MAX_RETRIES));
    }
}
