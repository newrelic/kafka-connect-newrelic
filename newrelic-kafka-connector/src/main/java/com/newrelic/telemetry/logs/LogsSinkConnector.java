package com.newrelic.telemetry.logs;

import com.newrelic.telemetry.TelemetrySinkConnector;
import org.apache.kafka.connect.connector.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogsSinkConnector extends TelemetrySinkConnector {
    private static Logger log = LoggerFactory.getLogger(LogsSinkConnector.class);

    @Override
    public Class<? extends Task> taskClass() {
        return LogsSinkTask.class;
    }
}
