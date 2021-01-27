package com.newrelic.telemetry.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.*;
import com.newrelic.telemetry.exceptions.DiscardBatchException;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.exceptions.RetryWithBackoffException;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.models.CountModel;
import com.newrelic.telemetry.metrics.models.GaugeModel;
import com.newrelic.telemetry.metrics.models.MetricModel;
import com.newrelic.telemetry.metrics.models.SummaryModel;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
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
    public int retriedCount;
    int retries;
    long retryInterval;

    public MetricBatchSender sender = null;

    MetricBuffer metricBuffer = null;


    @Override
    public String version() {
        return "1.1.0";
    }


    @Override
    public void start(Map<String, String> map) {

        String apiKey = map.get(TelemetrySinkConnectorConfig.API_KEY);
        retries = map.get(TelemetrySinkConnectorConfig.MAX_RETRIES) != null ? Integer.parseInt(map.get(TelemetrySinkConnectorConfig.MAX_RETRIES)) : (Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.MAX_RETRIES);
        retryInterval = map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS) != null ? Long.parseLong(map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS)) : (Long) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS);

        MetricBatchSenderFactory factory = MetricBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new);
        sender = factory.createBatchSender(apiKey);
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
                List<MetricModel> dataValues = (ArrayList<MetricModel>) record.value();
                for (MetricModel metricValue : dataValues) {
                    if(metricValue instanceof GaugeModel) {
                        GaugeModel gaugeModel =(GaugeModel) metricValue;
                        Gauge gauge = new Gauge(gaugeModel.name, gaugeModel.value, gaugeModel.timestamp, buildAttributes(gaugeModel.attributes));
                        log.debug("this is gauge " + gauge.toString());
                        metricBuffer.addMetric(gauge);
                    } else if(metricValue instanceof CountModel) {
                        CountModel countModel =(CountModel) metricValue;
                        Count count = new Count(countModel.name, countModel.value, countModel.timestamp, countModel.timestamp + countModel.interval, buildAttributes(countModel.attributes));
                        log.debug("this is count " + count.toString());
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
                        log.debug("this is count " + summary.toString());
                        metricBuffer.addMetric(summary);

                    }
                }

            } catch (IllegalArgumentException ie) {
                log.error(ie.getMessage());
                //throw ie;
                continue;
            }


        }
        MetricBatch metricBatch = metricBuffer.createBatch();
        if(metricBatch != null && !metricBatch.isEmpty()) {
            retriedCount = 0;
            while (retriedCount++ < retries - 1) {
                try {
                    sendToNewRelic(metricBatch);
                    break;
                }  catch(RetriableException re) {
                    log.info("Retrying for "+retriedCount+" time");
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



    private void sendToNewRelic(final MetricBatch metricBatch) {
        try {
            Response response = null;
            response = sender.sendBatch(metricBatch);
            log.debug("Response from new relic " + response);

            if (!(response.getStatusCode() == 200 || response.getStatusCode() == 202)) {
                log.error("New Relic sent back error " + response.getStatusMessage());
                throw new RetriableException(response.getStatusMessage());
            }
        } catch (RetryWithBackoffException re) {
            log.error("New Relic down " + re.getMessage());
            throw new RetriableException(re);
        } catch (DiscardBatchException re) {
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
