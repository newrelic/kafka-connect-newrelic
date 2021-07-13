package com.newrelic.telemetry;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TelemetrySinkConnector extends SinkConnector {
    private static Logger log = LoggerFactory.getLogger(TelemetrySinkConnector.class);
    private TelemetrySinkConnectorConfig config;
    private Map<String, String> configProps = new HashMap<>();

    @Override
    public String version() {
        return "2.0.0";
    }

    @Override
    public void start(Map<String, String> props) {
        configProps = props;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {

        final List<Map<String, String>> configs = new ArrayList<>(maxTasks);
        for (int i = 0; i < maxTasks; ++i) {
            configs.add(configProps);
        }

        return configs;
    }

    @Override
    public void stop() {
        //TODO: Do things that are necessary to stop your connector.
    }

    @Override
    public ConfigDef config() {
        return TelemetrySinkConnectorConfig.conf();
    }
}
