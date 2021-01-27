package com.newrelic.telemetry.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.Response;
import com.newrelic.telemetry.TelemetrySinkConnectorConfig;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.events.EventsConverter;
import com.newrelic.telemetry.events.TelemetryEventsSinkTask;
import com.newrelic.telemetry.exceptions.ResponseException;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TelemetryEventsSinkTaskTest {
    TelemetryEventsSinkTask sinkTask = new TelemetryEventsSinkTask();
    Map<String, String> configs = null;
    Response response = new Response(200, "Successful", null);
    ObjectMapper mapper = new ObjectMapper();

    private static final String eventJSON = "[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"timestamp\": 1593538862738,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\",\"isValid\": true}]";
    private static final String eventJSONTSMissing = "[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";
    private static final String eventJSONEventTypeMissing = "[{\"diskTotalBytes\": 11000000000,\"timestamp\": 1593538862738,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";

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
    public void testPutEvent() throws ResponseException {

        sinkTask.start(configs);
        sinkTask.eventSender = mock(EventBatchSender.class);
        sinkTask.retriedCount = 0;

        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue events = new EventsConverter().toConnectData(eventJSON, eventJSON.getBytes());

        records.add(new SinkRecord("test", 0, null, null, null, events.value(), 0));


        when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

        sinkTask.put(records);

        assertTrue(sinkTask.eventBuffer.getEvents().isEmpty());
        assertEquals(1, sinkTask.retriedCount);

    }

    @Test
    public void testPutEventWithBadNRURL() {
        sinkTask.NRURL = "https://insights-collector.newrelic.com12/v1/accounts/events";
        sinkTask.start(configs);

        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue events = new EventsConverter().toConnectData(eventJSON, eventJSON.getBytes());
        ;

        records.add(new SinkRecord("test", 0, null, null, null, events.value(), 0));

        try {
            sinkTask.put(records);
        } catch (ConnectException ce) {
            assertEquals(ce.getMessage(), "failed to connect to new relic after retries 5");
        }
    }

    @Test
    public void testPutEventWithBadNRURLWithDifferentRetries() {
        sinkTask.NRURL = "https://insights-collector.newrelic.com12/v1/accounts/events";
        configs.put(TelemetrySinkConnectorConfig.MAX_RETRIES, "3");
        sinkTask.start(configs);

        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue events = new EventsConverter().toConnectData(eventJSON, eventJSON.getBytes());
        ;

        records.add(new SinkRecord("test", 0, null, null, null, events.value(), 0));

        try {
            sinkTask.put(records);
        } catch (ConnectException ce) {
            assertEquals(ce.getMessage(), "failed to connect to new relic after retries 3");
        }
    }

    @Test(expected = ConnectException.class)
    public void testPutWithRetryApiResponse_ShouldThrowError() throws ResponseException, JsonProcessingException {
        Response tooManyResponse = new Response(429, "Please retry", null);
        sinkTask.start(configs);
        sinkTask.eventSender = mock(EventBatchSender.class);
        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue events = new EventsConverter().toConnectData(eventJSON, eventJSON.getBytes());
        records.add(new SinkRecord("test", 0, null, null, null, events.value(), 0));

        when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(tooManyResponse);
        sinkTask.put(records);
    }
}
