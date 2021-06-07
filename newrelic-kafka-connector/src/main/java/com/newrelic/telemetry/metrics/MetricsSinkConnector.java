package com.newrelic.telemetry.metrics;

import com.newrelic.telemetry.TelemetrySinkConnector;
import org.apache.kafka.connect.connector.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsSinkConnector extends SinkConnector {
    private static Logger log = LoggerFactory.getLogger(MetricsSinkConnector.class);

    @Override
    public Class<? extends Task> taskClass() {
        return MetricsSinkTask.class;
    }
}