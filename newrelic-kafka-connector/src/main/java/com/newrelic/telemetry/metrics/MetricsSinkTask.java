package com.newrelic.telemetry.metrics;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryBatch;
import com.newrelic.telemetry.TelemetrySinkTask;
import org.apache.kafka.connect.sink.SinkRecord;
import org.w3c.dom.Attr;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MetricsSinkTask extends TelemetrySinkTask<Metric> {

    private LinkedBlockingQueue<Metric> queue;

    private Attributes commonAttributes;

    public MetricsSinkTask() {
        super();
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public Metric createTelemetry(SinkRecord record) {
        return MetricConverter.toNewRelicMetric(record);
    }

    @Override
    public TelemetryBatch<Metric> createBatch(Collection<Metric> buffer, Attributes attributes) {
        return new MetricBatch(buffer, attributes);
    }

    @Override
    public BlockingQueue<Metric> getQueue() {
        return this.queue;
    }

}
