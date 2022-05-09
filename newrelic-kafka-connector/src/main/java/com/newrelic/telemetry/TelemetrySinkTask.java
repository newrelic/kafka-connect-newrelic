package com.newrelic.telemetry;

import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.logs.LogBatchSender;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.spans.SpanBatchSender;
import okhttp3.OkHttpClient;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public abstract class TelemetrySinkTask<T extends Telemetry> extends SinkTask {

    protected String nrClientProxyHost = (String) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_CLIENT_PROXY_HOST);

    protected Integer nrClientProxyPort = (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_CLIENT_PROXY_PORT);

    protected Integer nrClientTimeoutMs = (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_CLIENT_TIMEOUT_MS);

    protected Integer nrFlushMaxRecords = (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS);

    protected Integer nrFlushMaxIntervalMs = (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS);

    private static Logger log = LoggerFactory.getLogger(TelemetrySinkTask.class);

    private ExecutorService batchRunnerExecutor;

    private TelemetryClient telemetryClient;

    public TelemetrySinkTask() {
        this.batchRunnerExecutor = Executors.newSingleThreadExecutor();
    }

    public abstract T createTelemetry(SinkRecord record);

    public abstract TelemetryBatch<T> createBatch(Collection<T> buffer, Attributes attributes);

    public abstract BlockingQueue<T> getQueue();


    @Override
    public String version() {
        return "2.3.0";
    }

    final String INTEGRATION_NAME = "newrelic-kafka-connector";

    @Override
    public void start(Map<String, String> properties) {
        String apiKey = properties.get(TelemetrySinkConnectorConfig.API_KEY);
        String region = properties.get(TelemetrySinkConnectorConfig.NR_REGION);
        BlockingQueue queue = this.getQueue();
        // set this to true to log the requests and responses to the New Relic APIs
        BaseConfig bc = new BaseConfig(apiKey, false);

        if (properties.get(TelemetrySinkConnectorConfig.NR_CLIENT_PROXY_HOST) != null) {
            this.nrClientProxyHost = properties.get(TelemetrySinkConnectorConfig.NR_CLIENT_PROXY_HOST);
        }
        if (properties.get(TelemetrySinkConnectorConfig.NR_CLIENT_PROXY_PORT) != null) {
            this.nrClientProxyPort = Integer.parseInt(properties.get(TelemetrySinkConnectorConfig.NR_CLIENT_PROXY_PORT));
        }
        if (properties.get(TelemetrySinkConnectorConfig.NR_CLIENT_TIMEOUT_MS) != null) {
            this.nrClientTimeoutMs = Integer.parseInt(properties.get(TelemetrySinkConnectorConfig.NR_CLIENT_TIMEOUT_MS));
        }
        if (properties.get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS) != null) {
            this.nrFlushMaxRecords = Integer.parseInt(properties.get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_RECORDS));
        }
        if (properties.get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS) != null) {
            this.nrFlushMaxIntervalMs = Integer.parseInt(properties.get(TelemetrySinkConnectorConfig.NR_FLUSH_MAX_INTERVAL_MS));
        }

        Supplier<HttpPoster> supplier;
        if (this.nrClientProxyHost != null && this.nrClientProxyPort != null) {
            log.debug(String.format("creating an OkHttp client using proxy: %s:%s. timeout: %s", this.nrClientProxyHost, this.nrClientProxyPort, this.nrClientTimeoutMs));
            supplier = () -> {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.nrClientProxyHost, this.nrClientProxyPort));
                OkHttpClient client = new OkHttpClient.Builder().proxy(proxy).callTimeout(Duration.ofMillis(this.nrClientTimeoutMs)).build();
                return new OkHttpPoster(client);
            };
        } else {
            log.debug(String.format("creating an OkHttp client without a proxy.  timeout: %s", this.nrClientTimeoutMs));
            supplier = () -> new OkHttpPoster(Duration.ofMillis(this.nrClientTimeoutMs));
        }

        MetricBatchSender metricBatchSender = MetricBatchSender.create(MetricBatchSenderFactory.fromHttpImplementation(supplier).configureWith(bc).setRegion(region).build());
        SpanBatchSender spanBatchSender = SpanBatchSender.create(SpanBatchSenderFactory.fromHttpImplementation(supplier).configureWith(bc).setRegion(region).build());
        EventBatchSender eventBatchSender = EventBatchSender.create(EventBatchSenderFactory.fromHttpImplementation(supplier).configureWith(bc).setRegion(region).build());
        LogBatchSender logBatchSender = LogBatchSender.create(LogBatchSenderFactory.fromHttpImplementation(supplier).configureWith(bc).setRegion(region).build());
        this.telemetryClient = new TelemetryClient(metricBatchSender, spanBatchSender, eventBatchSender, logBatchSender);

        Attributes commonAttributes = new Attributes();
        commonAttributes.put("collector.name", INTEGRATION_NAME);
        commonAttributes.put("collector.version", this.version());
        TelemetryBatchRunner<T> batchRunner = new TelemetryBatchRunner<>(telemetryClient, this::createBatch, queue, this.nrFlushMaxRecords, this.nrFlushMaxIntervalMs, TimeUnit.MILLISECONDS, commonAttributes);


        this.batchRunnerExecutor.execute(batchRunner);

    }

    @Override
    public void put(Collection<SinkRecord> records) {

        for (SinkRecord record : records) {
            try {
                String keyStr = (record.key() == null) ? "[NULL]" : record.key().toString();

                if (record.value() == null) {
                    log.debug(String.format("Record with key %s had a null value.  Skipping."), keyStr);
                } else {
                    log.debug(String.format("processing record:\nkey:%s\nvalue:\n%s", keyStr, record.value().toString()));
                    T t = this.createTelemetry(record);
                    this.getQueue().add(t);
                }
            } catch (Exception e) {
                log.error("Caught exception while processing a message", e);
            }
        }


    }


    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> map) {
        super.flush(map);
    }

    @Override
    public void stop() {
        this.batchRunnerExecutor.shutdownNow();
        telemetryClient.shutdown();
    }

}
