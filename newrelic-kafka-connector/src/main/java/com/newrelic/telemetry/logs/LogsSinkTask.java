package com.newrelic.telemetry.logs;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryBatch;
import com.newrelic.telemetry.TelemetrySinkTask;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogsSinkTask extends TelemetrySinkTask<Log> {

    private LinkedBlockingQueue<Log> queue;

    public LogsSinkTask() {
        super();
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public Log createTelemetry(SinkRecord record) {
        Log l = LogConverter.toNewRelicLog(record);
        return new Log.LogBuilder()
                .timestamp(this.useRecordTimestamp ? record.timestamp() : l.getTimestamp())
                .message(l.getMessage())
                .attributes(l.getAttributes())
                .build();
    }

    @Override
    public TelemetryBatch<Log> createBatch(Collection<Log> buffer, Attributes attributes) {
        return new LogBatch(buffer, attributes);
    }

    @Override
    public BlockingQueue<Log> getQueue() {
        return this.queue;
    }
}