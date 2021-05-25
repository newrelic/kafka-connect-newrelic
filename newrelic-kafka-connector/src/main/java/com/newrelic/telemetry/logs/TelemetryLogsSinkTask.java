package com.newrelic.telemetry.logs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.*;
import com.newrelic.telemetry.logs.models.LogModel;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TelemetryLogsSinkTask extends SinkTask {
    private static Logger logger = LoggerFactory.getLogger(TelemetryLogsSinkTask.class);
    String apiKey = null;
    int retries;
    int timeout;    
    long retryInterval;
    boolean useRecordTimestamp;    
    private TelemetrySinkConnectorConfig connectorConfig;
    public LogBatchSender logSender = null;
    public String NRURL="https://log-api.newrelic.com/log/v1";
    public int retriedCount;
    LogBuffer logBuffer = null;

    public LogBatch logBatch = null;

    ObjectMapper mapper = null;


    @Override
    public String version() {
        return "1.0.0";
    }


    @Override
    public void start(Map<String, String> map) {
        connectorConfig = new TelemetrySinkConnectorConfig(map);
        useRecordTimestamp = connectorConfig.useRecordTimestamp;
        apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        retries = 3;
        retryInterval = 2000 ;
        timeout = 2;

        mapper = new ObjectMapper();

        try {
            LogBatchSenderFactory logFactory = LogBatchSenderFactory.fromHttpImplementation((Supplier<HttpPoster>) OkHttpPoster::new);
            logSender = LogBatchSender.create(logFactory.configureWith(apiKey).endpoint(new URL(NRURL)).auditLoggingEnabled(true).httpPoster(new OkHttpPoster(Duration.ofSeconds(timeout))).build());                
            logBuffer = new LogBuffer(new Attributes());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
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
            // Value is nested JSON
            else if (attributeValue instanceof LinkedHashMap) {
                try {
                    attributes.put(key, new ObjectMapper().writeValueAsString(attributeValue));
                } catch (JsonProcessingException e) {
                    //e.printStackTrace();
                    attributes.put(key, (String) attributeValue.toString());
                }
            }
            else if (attributeValue instanceof ArrayList) {
                // Changing ArrayList to a comma separated string
                String values;
                try {
                    values = new ObjectMapper().writeValueAsString(attributeValue);
                    values = values.substring(1, (values.length()-1)-1);
                    attributes.put(key, values);
                } catch (JsonProcessingException e) {
                    // e.printStackTrace();
                    attributes.put(key, (String) attributeValue.toString());
                }
            }
        });
        return attributes;
    }


    @Override
    public void put(Collection<SinkRecord> records) {
        for (SinkRecord record : records) {
            try {
                logger.debug("got back record " + record.toString());
                List<LogModel> dataValues = (List<LogModel>) record.value();
                dataValues.forEach(logModel -> {
                    Attributes attributes = buildAttributes(logModel.fields());
                    long timestamp;
                    Map<String, Object> map = attributes.asMap();                    
                    if(!map.containsKey("timestamp")) {
                        timestamp = System.currentTimeMillis();
                        if (connectorConfig.useRecordTimestamp && record.timestamp() != null) {
                            logger.debug("Using Record Timestamp: "+(long)(record.timestamp()));
                            timestamp = (long)(record.timestamp() / 1000.0); 
                        }    
                    } else {
                        timestamp = Long.parseLong((String)map.get("timestamp"));
                        logger.debug("Using timestamp found in log line: "+timestamp);                        
                    }

                    Log log = Log.builder()
                    .attributes(attributes)
                    .timestamp(timestamp)
                    .build();
                    logBuffer.addLog(log);
                });
            } catch (IllegalArgumentException ie) {
                logger.error(ie.getMessage());
                //throw ie;
                continue;
            }
        }
        if(!logBuffer.getLogs().isEmpty()) {
            retriedCount = 0;

                while (retriedCount++ < retries - 1) {
                    try {
                        logBatch = logBuffer.createBatch();
                        sendToNewRelic();
                        break;
                    }  catch(RetriableException re) {
                        logger.error("Retrying for "+retriedCount+" time");
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            logger.error("Retry Sleep thread was interrupted");
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
            logger.debug("logBatch (Size:"+logBatch.size()+") to be sent: "+ logBatch);
            response = logSender.sendBatch(logBatch);
            logger.debug("Response from new relic " + response);
            if (!(response.getStatusCode() == 200 || response.getStatusCode() == 202)) {
                logger.error("New Relic sent back error " + response.getStatusMessage());
                throw new RetriableException(response.getStatusMessage());
            }
        } catch (RetryWithBackoffException re) {
            logger.error("New Relic down " + re.getMessage()+"\nCurrent Batch : "+logBatch);
            throw new RetriableException(re);
        }  catch (DiscardBatchException re) {
            logger.error("DiscardBatchException : Make sure api.key is set to an Insights Insert Key. https://bit.ly/3cAvFVp Key: "+apiKey+" "+re.getMessage()+"\nBad Batch : "+logBatch.getTelemetry());
            throw new ConnectException(re);
        } catch (ResponseException re) {
            logger.error("ResponseException : "+re.getMessage()+"\nBad Batch : "+logBatch);
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
