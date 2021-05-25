package com.newrelic.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.logs.Log;
import com.newrelic.telemetry.logs.LogBatch;
import com.newrelic.telemetry.logs.LogBatchSender;
import com.newrelic.telemetry.logs.LogsConverter;
import com.newrelic.telemetry.logs.TelemetryLogsSinkTask;
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
import static org.mockito.Mockito.*;

public class TelemetryLogsSinkTaskTest {
    TelemetryLogsSinkTask sinkTask = new TelemetryLogsSinkTask();
    Map<String, String> configs = null;
    Response response = new Response(200, "Successful", null);
    ObjectMapper mapper = new ObjectMapper();

    private static final String logsJSON =            "{ \"key1\": \"value1\", \"key2\": \"value2\", \"key3\": \"value3\"}";
    private static final long TEST_TIMESTAMP = 1616638651294L;
    private static final String logsJSONTimestamp =   "{ \"key1\": \"value1\", \"key2\": \"value2\", \"key3\": \"value3\" , \"timestamp\": \""+TEST_TIMESTAMP+"\"}";    
    private static final String logsJSONNestedValue = "{ \"key1\": \"value1\", \"key2\": \"value2\", \"key3\": \"value3\", \"nestedkey\": {\"key1\": \"value4\",\"key2\": \"value5\"}}";

    private long logTimestamp = 0L;

    @Before
    public void init() {
        //sinkTask.mapper = mapper;
        configs = new HashMap<>();
        configs.put(TelemetrySinkConnectorConfig.API_KEY, "");
    }


  @Test
  public void testPutEvent() throws ResponseException{

    sinkTask.start(configs);
    sinkTask.logSender = mock(LogBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    SchemaAndValue  events =  new LogsConverter().toConnectData(logsJSON, logsJSON.getBytes());

    records.add(new SinkRecord("test",0,null, null, null, events.value(), 0 ));

    when(sinkTask.logSender.sendBatch(any(LogBatch.class))).thenReturn(response);

    sinkTask.put(records);

    assertEquals(1, sinkTask.logBatch.size());

  }
  
  @Test
  public void testPutEventWithNestedValue() throws ResponseException{

    sinkTask.start(configs);
    sinkTask.logSender = mock(LogBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    SchemaAndValue  events =  new LogsConverter().toConnectData(logsJSONNestedValue, logsJSONNestedValue.getBytes());

    records.add(new SinkRecord("test",0,null, null, null, events.value(), 0 ));

    when(sinkTask.logSender.sendBatch(any(LogBatch.class))).thenReturn(response);

    sinkTask.put(records);

    assertEquals(1, sinkTask.logBatch.size());
    Collection<Log> logs = (Collection<Log>)(sinkTask.logBatch).getTelemetry();
    logs.forEach(log -> {
      Map<String, Object> attr= log.getAttributes().asMap();
      assertEquals((String) attr.get("nestedkey"), "{\"key1\":\"value4\",\"key2\":\"value5\"}"); 
    });

  } 
  
  @Test
  public void testPutEventWithTimestamp() throws ResponseException{
    sinkTask.start(configs);
    sinkTask.logSender = mock(LogBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    SchemaAndValue  events =  new LogsConverter().toConnectData(logsJSONTimestamp, logsJSONTimestamp.getBytes());

    records.add(new SinkRecord("test",0,null, null, null, events.value(), 0 ));

    when(sinkTask.logSender.sendBatch(any(LogBatch.class))).thenReturn(response);

    sinkTask.put(records);

    Collection<Log> logs = (Collection<Log>)(sinkTask.logBatch).getTelemetry();
    logs.forEach(log -> {
      logTimestamp = log.getTimestamp();
    });

    assertEquals(1, sinkTask.logBatch.size());
    assertEquals(logTimestamp, TEST_TIMESTAMP);
  } 

  @Test
  public void testPutEventWithBadNRURL()  {
    sinkTask.NRURL="https://log-api.newrelic.com12/log/v1";
    sinkTask.start(configs);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    SchemaAndValue  events =  new LogsConverter().toConnectData(logsJSON, logsJSON.getBytes());

    records.add(new SinkRecord("test",0,null, null, null, events.value(), 0 ));

    try{
        sinkTask.put(records);
    } catch (ConnectException ce) {
        assertEquals(ce.getMessage(), "failed to connect to new relic after retries 5");
    }
  }

    @Test
    public void testPutEventWithBadNRURLWithDifferentRetries()  {
        sinkTask.NRURL="https://log-api.newrelic.com12/log/v1";

        sinkTask.start(configs);

        Collection<SinkRecord> records = new ArrayList<SinkRecord>();
        SchemaAndValue  events =  new LogsConverter().toConnectData(logsJSON, logsJSON.getBytes());;

        records.add(new SinkRecord("test",0,null, null, null, events.value(), 0 ));

        try{
            sinkTask.put(records);
        } catch (ConnectException ce) {
            assertEquals(ce.getMessage(), "failed to connect to new relic after retries 3");
        }
    }
}
