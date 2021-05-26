package com.newrelic.telemetry.events;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryBatch;
import com.newrelic.telemetry.TelemetrySinkTask;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventsSinkTask extends TelemetrySinkTask<Event> {

    private LinkedBlockingQueue<Event> queue;

    public EventsSinkTask() {
        super();
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public Event createTelemetry(SinkRecord record) {
        return EventConverter.toNewRelicEvent(record);
    }

    @Override
    public TelemetryBatch<Event> createBatch(Collection<Event> buffer, Attributes attributes) {
        return new EventBatch(buffer, attributes);
    }

    @Override
    public BlockingQueue<Event> getQueue() {
        return this.queue;
    }
}
