package com.newrelic.telemetry.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.*;
import com.newrelic.telemetry.exceptions.DiscardBatchException;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.exceptions.RetryWithBackoffException;
import com.newrelic.telemetry.http.HttpPoster;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class TelemetryEventsSinkTask extends SinkTask {
    private static Logger log = LoggerFactory.getLogger(TelemetryEventsSinkTask.class);
    String apiKey = null;
    int retries;
    int timeout;    
    long retryInterval;
    public EventBatchSender eventSender = null;
    public String NRURL="https://insights-collector.newrelic.com/v1/accounts/events";
    public int retriedCount;
    EventBuffer eventBuffer = null;

    public EventBatch eventBatch = null;

    ObjectMapper mapper = null;


    @Override
    public String version() {
        return "2.0";
    }


    @Override
    public void start(Map<String, String> map) {

        apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        retries = map.get(TelemetrySinkConnectorConfig.MAX_RETRIES) != null ? Integer.parseInt(map.get(TelemetrySinkConnectorConfig.MAX_RETRIES)) : (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.MAX_RETRIES);
        retryInterval = map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS) != null ? Long.parseLong(map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS)) : (Long) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS);
        timeout = map.get(TelemetrySinkConnectorConfig.TIMEOUT_SECONDS) != null ? Integer.parseInt(map.get(TelemetrySinkConnectorConfig.TIMEOUT_SECONDS)) : (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.TIMEOUT_SECONDS);        
        mapper = new ObjectMapper();

        try {
            EventBatchSenderFactory eventFactory = EventBatchSenderFactory.fromHttpImplementation((Supplier<HttpPoster>) OkHttpPoster::new);
            eventSender = EventBatchSender.create(eventFactory.configureWith(apiKey).endpoint(new URL(NRURL)).httpPoster(new OkHttpPoster(Duration.ofSeconds(timeout))).build());
            eventBuffer = new EventBuffer(new Attributes());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        for (SinkRecord record : records) {
            try {
                log.debug("got back record " + record.toString());
                eventBuffer.addEvent(EventConverter.toNewRelicEvent(record));
            } catch (IllegalArgumentException ie) {
                log.error(ie.getMessage());
                //throw ie;
                continue;
            }
        }
        if(!eventBuffer.getEvents().isEmpty()) {
            retriedCount = 0;

                while (retriedCount++ < retries - 1) {
                    try {
                        if(eventBatch==null)
                            eventBatch = eventBuffer.createBatch();
                        sendToNewRelic();
                        break;
                    }  catch(RetriableException re) {
                        log.error("Retrying for "+retriedCount+" time");
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            log.error("Retry Sleep thread was interrupted");
                        }

                    }
                }
                if(retriedCount==retries)
                    throw new ConnectException("failed to connect to new relic after retries "+retriedCount);
            }

        }


    private void sendToNewRelic() {
        try {
            Response response = null;
            response = eventSender.sendBatch(eventBatch);
            log.info("Response from new relic " + response);
            if (!(response.getStatusCode() == 200 || response.getStatusCode() == 202)) {
                log.error("New Relic sent back error " + response.getStatusMessage());
                throw new RetriableException(response.getStatusMessage());
            }
        } catch (RetryWithBackoffException re) {
            log.error("New Relic down " + re.getMessage());
            throw new RetriableException(re);
        }  catch (DiscardBatchException re) {
            log.error("API key is probably not right : "+re.getMessage());
            throw new ConnectException(re);
        } catch (ResponseException re) {
            log.error("API key is probably not right : "+re.getMessage());
            throw new ConnectException(re);
        }
    }


    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> map) {
        super.flush(map);
    }

    @Override
    public void stop() {
        //Close resources here.
    }

}
