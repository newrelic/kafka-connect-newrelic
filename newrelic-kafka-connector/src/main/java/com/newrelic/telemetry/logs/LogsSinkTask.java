package com.newrelic.telemetry.logs;

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
        return LogConverter.toNewRelicLog(record);
        //return null;
    }

    @Override
    public TelemetryBatch<Log> createBatch(Collection<Log> buffer) {
        // return new LogBatch(buffer);
        return null;
    }

    @Override
    public BlockingQueue<Log> getQueue() {
        // return this.queue;
        return null;
    }
}