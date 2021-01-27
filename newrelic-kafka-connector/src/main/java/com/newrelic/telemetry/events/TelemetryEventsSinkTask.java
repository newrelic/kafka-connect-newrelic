package com.newrelic.telemetry.events;

import com.newrelic.telemetry.*;
import com.newrelic.telemetry.events.models.EventModel;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TelemetryEventsSinkTask extends SinkTask {
    private static Logger log = LoggerFactory.getLogger(TelemetryEventsSinkTask.class);
    int retries;
    long retryInterval;
    public EventBatchSender eventSender = null;
    public int retriedCount;
    protected EventBuffer eventBuffer = null;

    @Override
    public String version() {
        return "1.1.0";
    }


    @Override
    public void start(Map<String, String> map) {

        String apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        retries = map.get(TelemetrySinkConnectorConfig.MAX_RETRIES) != null ? Integer.parseInt(map.get(TelemetrySinkConnectorConfig.MAX_RETRIES)) : (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.MAX_RETRIES);
        retryInterval = map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS) != null ? Long.parseLong(map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS)) : (Long) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS);

        EventBatchSenderFactory eventFactory = EventBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new);
        eventSender = eventFactory.createBatchSender(apiKey);
        eventBuffer = new EventBuffer(new Attributes());
    }

    private Attributes buildAttributes(Map<String, Object> atts) {
        Attributes attributes = new Attributes();
        atts.keySet().forEach(key -> {
            Object attributeValue = atts.get(key);
            if (attributeValue instanceof String)
                attributes.put(key, (String) attributeValue);
            else if (attributeValue instanceof Number)
                attributes.put(key, (Number) attributeValue);
            else if (attributeValue instanceof Boolean)
                attributes.put(key, (Boolean) attributeValue);

        });
        return attributes;
    }


    @Override
    public void put(Collection<SinkRecord> records) {
        for (SinkRecord record : records) {
            try {
                log.debug("got back record " + record.toString());
                List<EventModel> dataValues = (List<EventModel>) record.value();
                dataValues.forEach(eventModel -> {
                    Event event = new Event(eventModel.eventType, buildAttributes(eventModel.otherFields()), eventModel.timestamp);
                    eventBuffer.addEvent(event);
                });
            } catch (IllegalArgumentException ie) {
                log.error(ie.getMessage());
                //throw ie;
                continue;
            }
        }
        EventBatch eventBatch = eventBuffer.createBatch();
        if(eventBatch != null && !eventBatch.isEmpty()) {
            retriedCount = 0;
            while (retriedCount++ < retries - 1) {
                try {
                    sendToNewRelic(eventBatch);
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


    private void sendToNewRelic(final EventBatch eventBatch) {
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
