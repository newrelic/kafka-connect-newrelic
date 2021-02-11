package com.newrelic.telemetry;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;

import java.util.Map;


public class TelemetrySinkConnectorConfig extends AbstractConfig {

    public static final String API_KEY = "api.key";
    private static final String API_KEY_DOC = "API Key for New Relic.";
    public static final String MAX_RETRIES = "nr.max.retries";
    private static final String RETRIES_DOC = "Number of retries when New Relic servers are down.";

    public static final String RETRY_INTERVAL_MS = "nr.retry.interval.ms";
    private static final String RETRY_INTERVAL_MS_DOC = "Interval between reties in milliseconds.";

    public TelemetrySinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
        super(config, parsedConfig);
    }

    public TelemetrySinkConnectorConfig(Map<String, String> parsedConfig) {
        this(conf(), parsedConfig);
    }

    private static ConfigDef configDef = new ConfigDef()
            .define(API_KEY, Type.PASSWORD, Importance.HIGH, API_KEY_DOC)
            .define(MAX_RETRIES, Type.INT, 5, Importance.LOW, RETRIES_DOC)
            .define(RETRY_INTERVAL_MS, Type.LONG, 1000, Importance.LOW, RETRY_INTERVAL_MS_DOC);

    public static ConfigDef conf() {
        return configDef;
    }

}
