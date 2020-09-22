package com.newrelic.telemetry;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;

import java.util.Map;


public class TelemetrySinkConnectorConfig extends AbstractConfig {

    public static final String API_KEY = "api.key";
    private static final String API_KEY_DOC = "API Key for New Relic.";


    public TelemetrySinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
        super(config, parsedConfig);
    }

    public TelemetrySinkConnectorConfig(Map<String, String> parsedConfig) {
        this(conf(), parsedConfig);
    }

    public static ConfigDef conf() {
        ConfigDef configDef = new ConfigDef()
                .define(API_KEY, Type.PASSWORD, Importance.HIGH, API_KEY_DOC);

        return configDef;
    }

}
