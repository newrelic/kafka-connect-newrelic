package com.newrelic.telemetry;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public abstract class TelemetrySinkTask<T extends Telemetry> extends SinkTask {

    protected boolean useRecordTimestamp = (Boolean) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.USE_RECORD_TIMESTAMP);

    protected int nrFlushMaxRecords = (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS);

    protected int nrFlushMaxIntervalMs = (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS);

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
        // set this to true to log the requests and responses to the New Relic APIs
        BaseConfig bc = new BaseConfig(apiKey, false);
        TelemetryClient client = TelemetryClient.create(OkHttpPoster::new, bc);

        this.nrFlushMaxRecords = Optional.ofNullable(map.get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS)).map(Integer::parseInt).orElse((Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS));
        this.nrFlushMaxIntervalMs = Optional.ofNullable(map.get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS)).map(Integer::parseInt).orElse((Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS));;
        this.useRecordTimestamp = Optional.ofNullable(map.get(TelemetrySinkConnectorConfig.USE_RECORD_TIMESTAMP)).map(Boolean::parseBoolean).orElse((Boolean) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.USE_RECORD_TIMESTAMP));;

        Attributes commonAttributes = new Attributes();
        commonAttributes.put("collector.name", INTEGRATION_NAME);
        commonAttributes.put("collector.version", this.version());
        commonAttributes.put("instrumentation.name", INTEGRATION_NAME);
        commonAttributes.put("instrumentation.version", this.version());
        commonAttributes.put("instrumentation.provider", "newRelic");
        TelemetryBatchRunner<T> batchRunner = new TelemetryBatchRunner<>(client, this::createBatch, queue, this.nrFlushMaxRecords, this.nrFlushMaxIntervalMs, TimeUnit.MILLISECONDS, commonAttributes);


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
