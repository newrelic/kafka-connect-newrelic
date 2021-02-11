package com.newrelic.telemetry.events;

import com.newrelic.telemetry.TelemetrySinkConnectorConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelemetryEventsSinkConnector extends SinkConnector {
    private static Logger log = LoggerFactory.getLogger(TelemetryEventsSinkConnector.class);
    private Map<String, String> configProps = new HashMap<>();

    @Override
    public String version() {
        return "1.1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        configProps = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        //TODO: Return your task implementation.
        return TelemetryEventsSinkTask.class;
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
