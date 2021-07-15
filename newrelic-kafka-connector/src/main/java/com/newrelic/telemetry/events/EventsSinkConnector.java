package com.newrelic.telemetry.events;

import com.newrelic.telemetry.TelemetrySinkConnector;
import org.apache.kafka.connect.connector.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventsSinkConnector extends TelemetrySinkConnector {
    private static Logger log = LoggerFactory.getLogger(EventsSinkConnector.class);

    @Override
    public Class<? extends Task> taskClass() {
        return EventsSinkTask.class;
    }
}
