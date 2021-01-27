package com.newrelic.telemetry;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;

import java.util.Map;


public class TelemetrySinkConnectorConfig extends AbstractConfig {

    public static final String API_KEY = "api.key";
    private static final String API_KEY_DOC = "API Key for New Relic.";

    public static final String NR_CLIENT_PROXY_HOST = "nr.client.proxy.host";
    private static final String NR_CLIENT_PROXY_HOST_DOC = "Proxy host to use to connect to the New Relic API";

    public static final String NR_CLIENT_PROXY_PORT= "nr.client.proxy.port";
    private static final String NR_CLIENT_PROXY_PORT_DOC = "Proxy port to use to connect to the New Relic API";

    public static final String NR_CLIENT_TIMEOUT_MS = "nr.client.timeout";
    private static final String NR_CLIENT_TIMEOUT_MS_DOC = "Time, in milliseconds, to wait for a response from the New Relic API (default is 2000)";


    public static final String NR_FLUSH_MAX_RECORDS = "nr.flush.max.records";
    private static final String NR_FLUSH_MAX_RECORDS_DOC = "The maximum number of records to send in a payload to New Relic (default 1000)";

    public static final String NR_FLUSH_MAX_INTERVAL_MS = "nr.flush.max.interval.ms";
    private static final String NR_FLUSH_MAX_INTERVAL_MS_DOC = "Maximum amount of time, in milliseconds, to wait before flushing records to New Relic (default 5000)";

    public TelemetrySinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
        super(config, parsedConfig);
    }

    public TelemetrySinkConnectorConfig(Map<String, String> parsedConfig) {
        this(conf(), parsedConfig);
    }

    private static ConfigDef configDef = new ConfigDef()
            .define(API_KEY, Type.PASSWORD, ConfigDef.NO_DEFAULT_VALUE, new ConfigDef.NonEmptyString(), Importance.HIGH, API_KEY_DOC)
            .define(MAX_RETRIES, Type.INT, 5, Importance.LOW, RETRIES_DOC)
            .define(RETRY_INTERVAL_MS, Type.LONG, 1000, Importance.LOW, RETRY_INTERVAL_MS_DOC);

    public static ConfigDef conf() {
        ConfigDef configDef = new ConfigDef()
                .define(API_KEY, Type.PASSWORD, Importance.HIGH, API_KEY_DOC)
                .define(NR_CLIENT_TIMEOUT_MS, Type.INT, 2000, Importance.LOW, NR_CLIENT_TIMEOUT_MS_DOC)
                .define(NR_CLIENT_PROXY_HOST, Type.STRING, null, Importance.LOW, NR_CLIENT_PROXY_HOST_DOC)
                .define(NR_CLIENT_PROXY_PORT, Type.INT, null, Importance.LOW, NR_CLIENT_PROXY_PORT_DOC)
                .define(NR_FLUSH_MAX_RECORDS, Type.INT, 1000, Importance.LOW, NR_FLUSH_MAX_RECORDS_DOC)
                .define(NR_FLUSH_MAX_INTERVAL_MS, Type.INT, 5000, Importance.LOW, NR_FLUSH_MAX_INTERVAL_MS_DOC);
        return configDef;
    }

}
