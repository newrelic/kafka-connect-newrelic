package com.newrelic.telemetry.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.Response;
import com.newrelic.telemetry.TelemetrySinkConnectorConfig;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricsConverter;
import com.newrelic.telemetry.metrics.TelemetryMetricsSinkTask;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TelemetryMetricsSinkTaskTest {
    TelemetryMetricsSinkTask sinkTask = new TelemetryMetricsSinkTask();
    Map<String, String> configs = null;
    Response response = new Response(200, "Successful", null);

    ObjectMapper mapper = new ObjectMapper();

    private static final String metricCountJSON = "[{\"metrics\": [{\"name\": \"cache.misses\",\"type\": \"count\",\"value\": 15,\"timestamp\": 1593448389023,\"interval.ms\": 10000,\"attributes\": {\"cache.name\": \"myCache\",\"host.name\":\"dev.server.com\",\"host.id\": 123456,\"host.isVirtual\": true } }]}]";

    private static final String metricCountGaugeSummaryJSON = "[{\"metrics\": [{\"name\": \"cache.misses\",\"type\": \"count\",\"value\": 15,\"timestamp\": 1593448389023,\"interval.ms\": 10000,\"attributes\": {\"cache.name\": \"myCache\",\"host.name\":\"dev.server.com\" } }, { \"name\": \"temperature\", \"type\": \"gauge\", \"value\": 15, \"timestamp\": 1593448389023, \"attributes\": { \"city\": \"Portland\",  \"state\": \"Oregon\"  } },{\"name\": \"service.response.duration\",\"type\": \"summary\", \"value\": {\"count\": 5,  \"sum\": 0.004382655, \"min\": 0.0005093, \"max\": 0.001708826},\"interval.ms\": 10000, \"timestamp\": 1593448389023,\"attributes\": {\"host.name\": \"dev.server.com\",\"app.name\": \"foo\",\"app.id\": 123456,\"app.isVirtual\": true}}]}]";

    private static final String metricWithCommonJSON = "[ { \"common\" : { \"timestamp\": 1593474263285, \"interval.ms\": 10000, \"attributes\": { \"app.name\": \"foo\", \"host.name\": \"dev.server.com\" } }, \"metrics\": [ { \"name\": \"service.errors.all\", \"type\": \"count\", \"value\": 9, \"attributes\": { \"service.response.statuscode\": \"400\" } }, { \"name\": \"service.errors.all\", \"type\": \"count\", \"value\": 4, \"attributes\": { \"service.response.statuscode\": \"500\" } }, { \"name\": \"service.response.duration\", \"type\": \"summary\", \"value\": { \"count\": 5, \"sum\": 0.004382655, \"min\": 0.0005093, \"max\": 0.001708826 }, \"attributes\": { \"service.response.statuscode\": \"200\",\"service.id\": 123456,\"server.isVirtual\": true } } ] } ]";


    @Before
    public void init() {
        //sinkTask.mapper = mapper;
        configs = new HashMap<>();
        configs.put(TelemetrySinkConnectorConfig.API_KEY, "");

    }

    @Test
    public void testVersion() {
        assertTrue(sinkTask.version().length() > 0);
    }

    @Test
    public void testPutMetricCount() throws ResponseException, JsonProcessingException {

        sinkTask.start(configs);
        sinkTask.sender = mock(MetricBatchSender.class);
        sinkTask.retriedCount = 0;
        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue metricsList = new MetricsConverter().toConnectData(metricCountJSON, metricCountJSON.getBytes());

        records.add(new SinkRecord("test", 0, null, null, null, metricsList.value(), 0));
        when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

        sinkTask.put(records);
        assertTrue(sinkTask.metricBuffer.getMetrics().isEmpty());
        assertEquals(1, sinkTask.retriedCount);

    }

    @Test
    public void testPutMetricCountGaugeSummary() throws ResponseException, JsonProcessingException {

        sinkTask.start(configs);
        sinkTask.sender = mock(MetricBatchSender.class);
        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue metricsList = new MetricsConverter().toConnectData(metricCountGaugeSummaryJSON, metricCountGaugeSummaryJSON.getBytes());


        records.add(new SinkRecord("test", 0, null, null, null, metricsList.value(), 0));
        when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

        sinkTask.put(records);
        assertTrue(sinkTask.metricBuffer.getMetrics().isEmpty());

    }

    @Test
    public void testPutMetricWithCommons() throws ResponseException, JsonProcessingException {
        sinkTask.start(configs);
        sinkTask.sender = mock(MetricBatchSender.class);

        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue metricsList = new MetricsConverter().toConnectData(metricWithCommonJSON, metricWithCommonJSON.getBytes());


        records.add(new SinkRecord("test", 0, null, null, null, metricsList.value(), 0));
        when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

        sinkTask.put(records);
        assertTrue(sinkTask.metricBuffer.getMetrics().isEmpty());

    }


    @Test(expected = ConnectException.class)
    public void testPutWithRetryApiResponse_ShouldThrowError() throws ResponseException, JsonProcessingException {
        Response tooManyResponse = new Response(429, "Please retry", null);

        sinkTask.start(configs);
        sinkTask.sender = mock(MetricBatchSender.class);
        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue metricsList = new MetricsConverter().toConnectData(metricCountJSON, metricCountJSON.getBytes());

        records.add(new SinkRecord("test", 0, null, null, null, metricsList.value(), 0));
        when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(tooManyResponse);

        sinkTask.put(records);
    }

}
