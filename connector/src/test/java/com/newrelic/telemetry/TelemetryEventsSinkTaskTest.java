package com.newrelic.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.events.TelemetryEventsSinkTask;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.metrics.*;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TelemetryEventsSinkTaskTest {
  TelemetryEventsSinkTask sinkTask = new TelemetryEventsSinkTask();
  Map<String, String> configs = null;
  Response response = new Response(200,"Successful", null);
  ObjectMapper mapper = new ObjectMapper();

  private  static final String eventJSON = "[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"timestamp\": 1593538862738,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";
  private  static final String eventJSONTSMissing = "[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";
  private  static final String eventJSONEventTypeMissing = "[{\"diskTotalBytes\": 11000000000,\"timestamp\": 1593538862738,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";

  @Before
  public void init(){
    //sinkTask.mapper = mapper;
    configs = new HashMap<>();
    configs.put (TelemetrySinkConnectorConfig.API_KEY,"");
    configs.put(TelemetrySinkConnectorConfig.ACCOUNT_ID,"123");
    configs.put(TelemetrySinkConnectorConfig.MAX_RETRIES, "5");
    configs.put(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS, "1000");
  }


  /*@Test
  public void testPutEvent() throws ResponseException, JsonProcessingException {
    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  events = mapper.readValue(eventJSON, List.class);

    records.add(new SinkRecord("test",0,null, null, null, events, 0 ));



    when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

    sinkTask.put(records);

    assertEquals(1, sinkTask.eventBatch.size());

  }

  //Timestamp missing should not throw exception as the exception handling block should catch it
  @Test
  public void testPutEventTimestampMissing() throws ResponseException, JsonProcessingException {
    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  events = mapper.readValue(eventJSONTSMissing, List.class);

    records.add(new SinkRecord("test",0,null, null, null, events, 0 ));

    when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

    sinkTask.put(records);


  }

  //Eventtype missing should not throw exception
  @Test
  public void testPutEventEventtypeMissing() throws ResponseException, JsonProcessingException {
    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  events = mapper.readValue(eventJSONEventTypeMissing, List.class);

    records.add(new SinkRecord("test",0,null, null, null, events, 0 ));

    when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

    sinkTask.put(records);
    //assertNull(sinkTask.eventBatch);
  }

  @Test(expected = RuntimeException.class)
  public void testPutEventWithBadNRURL() throws ResponseException, JsonProcessingException {

    sinkTask.start(configs);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  events = mapper.readValue(eventJSON, List.class);

    records.add(new SinkRecord("test",0,null, null, null, events, 0 ));

    //when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(null);

    sinkTask.put(records);
    assertEquals(1, sinkTask.eventBatch.size());

  }*/
}
