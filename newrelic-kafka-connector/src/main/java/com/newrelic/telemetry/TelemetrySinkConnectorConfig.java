package com.newrelic.telemetry;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;

import java.util.Map;


public class TelemetrySinkConnectorConfig extends AbstractConfig {

    public static final String API_KEY = "api.key";
    private static final String API_KEY_DOC = "API Key for New Relic.";

    public static final String USE_RECORD_TIMESTAMP = "use.record.timestamp";    
    private static final String USE_RECORD_TIMESTAMP_DOC = "When set to `true`, The timestamp "
    + "is retrieved from the Kafka record and passed to New Relic. When set to false, the timestamp will be the ingestion timestamp. By "
    + "default, this is set to true.";

    public static final String NR_FLUSH_MAX_RECORDS = "nr.flush.max.records";
    private static final String NR_FLUSH_MAX_RECORDS_DOC = "The maximum number of records to send in a payload";

    public static final String NR_FLUSH_MAX_INTERVAL_MS = "nr.flush.max.interval.ms";
    private static final String NR_FLUSH_MAX_INTERVAL_MS_DOC = "Maximum amount of time to wait before flushing if ";
    public final boolean useRecordTimestamp;

    public TelemetrySinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
        super(config, parsedConfig);
        useRecordTimestamp = getBoolean(USE_RECORD_TIMESTAMP);
    }

    public TelemetrySinkConnectorConfig(Map<String, String> parsedConfig) {
        this(conf(), parsedConfig);
    }

    public static ConfigDef conf() {
        ConfigDef configDef = new ConfigDef()
                .define(API_KEY, Type.PASSWORD, Importance.HIGH, API_KEY_DOC)
                .define(USE_RECORD_TIMESTAMP, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM, USE_RECORD_TIMESTAMP_DOC)
                .define(NR_FLUSH_MAX_RECORDS, Type.INT, 1000, Importance.LOW, NR_FLUSH_MAX_RECORDS_DOC)
                .define(NR_FLUSH_MAX_INTERVAL_MS, Type.INT, 5000, Importance.LOW, NR_FLUSH_MAX_INTERVAL_MS_DOC);
        return configDef;
    }

}
