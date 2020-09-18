package com.newrelic.telemetry.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.*;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.models.CountModel;
import com.newrelic.telemetry.metrics.models.GaugeModel;
import com.newrelic.telemetry.metrics.models.MetricModel;
import com.newrelic.telemetry.metrics.models.SummaryModel;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TelemetryMetricsSinkTask extends SinkTask {
    private static Logger log = LoggerFactory.getLogger(TelemetryMetricsSinkTask.class);
    String apiKey = null;
    String dataType = null;
    //long accountId=0l;
    public MetricBatchSender sender = null;

    MetricBuffer metricBuffer = null;

    public MetricBatch metricBatch = null;

    ObjectMapper mapper = null;

    int retries = 0;
    long retryInterval = 0L;
    int retriedCount = 0;

    @Override
    public String version() {
        return "1.0.0";
    }


    @Override
    public void start(Map<String, String> map) {

        apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        retries = map.get(TelemetrySinkConnectorConfig.MAX_RETRIES) != null ? Integer.parseInt(map.get(TelemetrySinkConnectorConfig.MAX_RETRIES)) : (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.MAX_RETRIES);
        retryInterval = map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS) != null ? Long.parseLong(map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS)) : (Long) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS);
        mapper = new ObjectMapper();
        MetricBatchSenderFactory factory =
                MetricBatchSenderFactory.fromHttpImplementation((Supplier<HttpPoster>) OkHttpPoster::new);
        log.info("this is the api key " + apiKey);
        sender =
                MetricBatchSender.create(factory.configureWith(apiKey).build());
        metricBuffer = new MetricBuffer(getCommonAttributes());
    }

    /**
     * These attributes are shared across all metrics submitted in the batch.
     */
    private static Attributes getCommonAttributes() {
        return new Attributes();
    }

    private Attributes buildAttributes(Map<String, Object> atts) {
        Attributes attributes = new Attributes();
        atts.keySet().forEach(key -> {
            //if(!(key.equals("timestamp") || key.equals("eventType"))) {
            Object attributeValue = atts.get(key);
            if (attributeValue instanceof String)
                attributes.put(key, (String) attributeValue);
            else if (attributeValue instanceof Number)
                attributes.put(key, (Number) attributeValue);
            else if (attributeValue instanceof Boolean)
                attributes.put(key, (Boolean) attributeValue);
            //}

        });
        return attributes;
    }


    @Override
    public void put(Collection<SinkRecord> records) {


        for (SinkRecord record : records) {
            try {
                log.info("got back record " + record.toString());
                List<MetricModel> dataValues = (ArrayList<MetricModel>) record.value();
                for (MetricModel metricValue : dataValues) {
                    if(metricValue instanceof GaugeModel) {
                        GaugeModel gaugeModel =(GaugeModel) metricValue;
                        Gauge gauge = new Gauge(gaugeModel.name, gaugeModel.value, gaugeModel.timestamp, buildAttributes(gaugeModel.attributes));
                        log.info("this is gauge " + gauge.toString());
                        metricBuffer.addMetric(gauge);
                    } else if(metricValue instanceof CountModel) {
                        CountModel countModel =(CountModel) metricValue;
                        Count count = new Count(countModel.name, countModel.value, countModel.timestamp, countModel.timestamp + countModel.interval, buildAttributes(countModel.attributes));
                        log.info("this is count " + count.toString());
                        metricBuffer.addMetric(count);
                    } else if(metricValue instanceof SummaryModel) {
                        SummaryModel summaryModel = (SummaryModel) metricValue;
                        Summary summary =
                                new Summary(summaryModel.name,
                                        summaryModel.value.count,
                                        summaryModel.value.sum,
                                        summaryModel.value.min,
                                        summaryModel.value.max,
                                        summaryModel.timestamp,
                                        summaryModel.timestamp + summaryModel.interval,
                                        buildAttributes(summaryModel.attributes));
                        log.info("this is count " + summary.toString());
                        metricBuffer.addMetric(summary);
                    }
                }

            } catch (IllegalArgumentException ie) {
                log.error(ie.getMessage());
                //throw ie;
                continue;
            }


        }

        metricBatch = metricBuffer.createBatch();

        retriedCount = 0;
        while (!circuitBreaker()) {
            if (retriedCount++ < retries - 1) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    log.error("Sleep thread was interrupted");
                }
            } else
                throw new ConnectException("failed to connect to new relic after retries");
        }

    }

    private boolean circuitBreaker() {
        try {
            sendToNewRelic();
            return true;
        } catch (ConnectException e) {
            return false;
        }
    }

    private void sendToNewRelic() {
        try {
            Response response = null;
            response = sender.sendBatch(metricBatch);
            log.info("Response from new relic " + response);

            if (!(response.getStatusCode() == 200 || response.getStatusCode() == 202)) {
                log.error("New Relic sent back error " + response.getStatusMessage());
                throw new ConnectException(response.getStatusMessage());
            }
        } catch (ResponseException re) {
            log.error("New Relic down " + re.getMessage());
            throw new ConnectException(re);
        }
    }

    private void logMalformedError(Map<String, Object> values) {
        log.error(dataType + " is malformed " + values.toString());
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> map) {

    }

    @Override
    public void stop() {
        //Close resources here.
    }

}
