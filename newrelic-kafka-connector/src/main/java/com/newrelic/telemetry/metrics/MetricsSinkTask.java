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
        Metric m = MetricConverter.toNewRelicMetric(record);

        // TODO - figure this out, we are back at the abstract level, can't get metricType or attributes from Metric.

        // if (this.useRecordTimestamp) {
        //     // TODO: return speciic event type
        //     // return new Metric(e.getMetricType(), e.getAttributes(), record.timestamp());
        //     return null;
        // } else {
        //     return e;
        // }

        // just use system timestamp for now
        return m;

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
