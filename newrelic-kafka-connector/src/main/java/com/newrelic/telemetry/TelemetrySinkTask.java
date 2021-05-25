package com.newrelic.telemetry;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public abstract class TelemetrySinkTask<T extends Telemetry> extends SinkTask {
    private static Logger log = LoggerFactory.getLogger(TelemetrySinkTask.class);

    private ExecutorService batchRunnerExecutor;

    public TelemetrySinkTask() {
        this.batchRunnerExecutor = Executors.newSingleThreadExecutor();
    }

    public abstract T createTelemetry(SinkRecord record);

    public abstract TelemetryBatch<T> createBatch(Collection<T> buffer);

    public abstract BlockingQueue<T> getQueue();

    @Override
    public String version() {
        return "2.0";
    }


    @Override
    public void start(Map<String, String> map) {
        String apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        BlockingQueue queue = this.getQueue();
        TelemetryClient client = TelemetryClient.create(OkHttpPoster::new, apiKey);

        int nrFlushMaxRecords = Integer.parseInt(map.getOrDefault(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS, (String) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS)));
        long nrFlushMaxIntervalMs = Long.parseLong(map.getOrDefault(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS, (String) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS)));

        TelemetryBatchRunner<T> batchRunner = new TelemetryBatchRunner<>(client, this::createBatch, queue, nrFlushMaxRecords, nrFlushMaxIntervalMs, TimeUnit.MILLISECONDS);

        this.batchRunnerExecutor.execute(batchRunner);

    }

    @Override
    public void put(Collection<SinkRecord> records) {

        for (SinkRecord record : records) {

            T t = this.createTelemetry(record);

            this.getQueue().add(t);

        }


    }


    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> map) {
        super.flush(map);
    }

    @Override
    public void stop() {
        this.batchRunnerExecutor.shutdown();
    }

}
