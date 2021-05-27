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

    public abstract TelemetryBatch<T> createBatch(Collection<T> buffer, Attributes attributes);

    public abstract BlockingQueue<T> getQueue();

    @Override
    public String version() {
        return "2.0";
    }

    final String INTEGRATION_NAME = "newrelic-kafka-connector";

    @Override
    public void start(Map<String, String> map) {
        String apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        BlockingQueue queue = this.getQueue();
        BaseConfig bc = new BaseConfig(apiKey, true);
        TelemetryClient client = TelemetryClient.create(OkHttpPoster::new, bc);

        int nrFlushMaxRecords = 1000;// (Integer) map.getOrDefault(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS, TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS));
        long nrFlushMaxIntervalMs = 5000; //Long.parseLong(map.getOrDefault(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS, (String) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS)));

        Attributes commonAttributes = new Attributes();
        commonAttributes.put("collector.name", INTEGRATION_NAME);
        commonAttributes.put("collector.version", this.version());
        commonAttributes.put("instrumentation.name", INTEGRATION_NAME);
        commonAttributes.put("instrumentation.version", this.version());
        commonAttributes.put("instrumentation.provider", "newRelic");
        TelemetryBatchRunner<T> batchRunner = new TelemetryBatchRunner<>(client, this::createBatch, queue, nrFlushMaxRecords, nrFlushMaxIntervalMs, TimeUnit.MILLISECONDS, commonAttributes);


        this.batchRunnerExecutor.execute(batchRunner);

    }

    @Override
    public void put(Collection<SinkRecord> records) {

        for (SinkRecord record : records) {

            log.debug(String.format("processing record: \n%s", record.value().toString()));
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
        this.batchRunnerExecutor.shutdownNow();
    }

}
