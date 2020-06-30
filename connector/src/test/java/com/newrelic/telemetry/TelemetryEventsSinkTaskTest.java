package com.newrelic.telemetry;

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
  public void testPutMetricCount() throws ResponseException {

    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);
    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    valueMap.put("name","cache.misses");
    valueMap.put("type","count");
    valueMap.put("value",15l);
    valueMap.put("timestamp",Long.valueOf(1592841990366l));
    valueMap.put("interval.ms",Long.valueOf(1000l) );
    Map<String, Object> attributesMap = new HashMap<>();
    attributesMap.put("cache.name", "myCache");
    attributesMap.put("host.name","dev.server.com");
    valueMap.put("attributes",attributesMap);
    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));
    when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

    sinkTask.put(records);
    assertEquals(1, sinkTask.metricBatch.size());

  }

  @Test
  public void testPutMetricSummary() throws ResponseException {
    configs.put(TelemetrySinkConnectorConfig.DATA_TYPE,"metric");
    sinkTask.start(configs);
    sinkTask.sender = mock(MetricBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    valueMap.put("name","cache.misses");
    valueMap.put("type","summary");
    Map<String, Object> valuesMap = new HashMap<>();
    valuesMap.put("count", 5l);
    valuesMap.put("sum", 0.004382655);
    valuesMap.put("min", 0.004382655);
    valuesMap.put("max", 0.001708826);
    valueMap.put("value",valuesMap);
    valueMap.put("timestamp",Long.valueOf(1592841990366l));
    valueMap.put("interval.ms",Long.valueOf(1000l) );
    Map<String, Object> attributesMap = new HashMap<>();
    attributesMap.put("cache.name", "myCache");
    attributesMap.put("host.name","dev.server.com");
    valueMap.put("attributes",attributesMap);
    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));
    when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

    sinkTask.put(records);
    assertEquals(1, sinkTask.metricBatch.size());

  }

  @Test
  public void testPutMetricGauge() throws ResponseException {
    configs.put(TelemetrySinkConnectorConfig.DATA_TYPE,"metric");
    sinkTask.start(configs);
    sinkTask.sender = mock(MetricBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    valueMap.put("name","cache.misses");
    valueMap.put("type","gauge");
    valueMap.put("value",15l);
    valueMap.put("timestamp",Long.valueOf(1592841990366l));
    Map<String, Object> attributesMap = new HashMap<>();
    attributesMap.put("cache.name", "myCache");
    attributesMap.put("host.name","dev.server.com");
    valueMap.put("attributes",attributesMap);
    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));
    when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

    sinkTask.put(records);
    assertEquals(1, sinkTask.metricBatch.size());

  }*/



  @Test
  public void testPutEvent() throws ResponseException {
    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    valueMap.put("eventType","SystemSample");
    valueMap.put("name","cache.misses");
    valueMap.put("type","count");
    valueMap.put("value",15l);
    valueMap.put("timestamp",Long.valueOf(1592841990366l));
    valueMap.put("interval.ms",Long.valueOf(1000l) );
    Map<String, Object> attributesMap = new HashMap<>();
    attributesMap.put("cache.name", "myCache");
    attributesMap.put("host.name","dev.server.com");
    valueMap.put("attributes",attributesMap);
    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));



    when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

    sinkTask.put(records);

    assertEquals(1, sinkTask.eventBatch.size());

  }

  //Timestamp missing should not throw exception as the exception handling block should catch it
  @Test
  public void testPutEventTimestampMissing() throws ResponseException {
    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    valueMap.put("eventType","SystemSample");
    valueMap.put("type","count");
    valueMap.put("value",15l);
    //valueMap.put("timestamp",Long.valueOf(1592841990366l));
    valueMap.put("interval.ms",Long.valueOf(1000l) );

    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));

    when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

    sinkTask.put(records);


  }

  //Eventtype missing should not throw exception
  @Test
  public void testPutEventEventtypeMissing() throws ResponseException {
    sinkTask.start(configs);
    sinkTask.eventSender = mock(EventBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    //valueMap.put("eventType","SystemSample");
    valueMap.put("type","count");
    valueMap.put("value",15l);
    valueMap.put("timestamp",Long.valueOf(1592841990366l));
    valueMap.put("interval.ms",Long.valueOf(1000l) );

    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));

    when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(response);

    sinkTask.put(records);
    //assertNull(sinkTask.eventBatch);
  }

  @Test(expected = RuntimeException.class)
  public void testPutEventWithBadNRURL() throws ResponseException {

    sinkTask.start(configs);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    Map<String, Object> valueMap = new HashMap<>();
    List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
    valueMap.put("eventType","SystemSample");
    valueMap.put("name","cache.misses");
    valueMap.put("type","count");
    valueMap.put("value",15l);
    valueMap.put("timestamp",Long.valueOf(1592841990366l));
    valueMap.put("interval.ms",Long.valueOf(1000l) );
    Map<String, Object> attributesMap = new HashMap<>();
    attributesMap.put("cache.name", "myCache");
    attributesMap.put("host.name","dev.server.com");
    valueMap.put("attributes",attributesMap);
    metrics.add(valueMap);
    records.add(new SinkRecord("test",0,null, null, null, metrics, 0 ));

    //when(sinkTask.eventSender.sendBatch(any(EventBatch.class))).thenReturn(null);

    sinkTask.put(records);
    assertEquals(1, sinkTask.eventBatch.size());

  }
}
