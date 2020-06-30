package com.newrelic.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.events.TelemetryEventsSinkTask;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.TelemetryMetricsSinkTask;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TelemetryMetricsSinkTaskTest {
  TelemetryMetricsSinkTask sinkTask = new TelemetryMetricsSinkTask();
  Map<String, String> configs = null;
  Response response = new Response(200,"Successful", null);

  ObjectMapper mapper = new ObjectMapper();

  private  static final String metricCountJSON = "[{\"metrics\": [{\"name\": \"cache.misses\",\"type\": \"count\",\"value\": 15,\"timestamp\": 1593448389023,\"interval.ms\": 10000,\"attributes\": {\"cache.name\": \"myCache\",\"host.name\":\"dev.server.com\" } }]}]";

  private  static final String metricCountGaugeSummaryJSON = "[{\"metrics\": [{\"name\": \"cache.misses\",\"type\": \"count\",\"value\": 15,\"timestamp\": 1593448389023,\"interval.ms\": 10000,\"attributes\": {\"cache.name\": \"myCache\",\"host.name\":\"dev.server.com\" } }, { \"name\": \"temperature\", \"type\": \"gauge\", \"value\": 15, \"timestamp\": 1593448389023, \"attributes\": { \"city\": \"Portland\",  \"state\": \"Oregon\"  } },{\"name\": \"service.response.duration\",\"type\": \"summary\", \"value\": {\"count\": 5,  \"sum\": 0.004382655, \"min\": 0.0005093, \"max\": 0.001708826},\"interval.ms\": 10000, \"timestamp\": 1593448389023,\"attributes\": {\"host.name\": \"dev.server.com\",\"app.name\": \"foo\"}}]}]";

  private  static final String metricWithCommonJSON = "[ { \"common\" : { \"timestamp\": 1593474263285, \"interval.ms\": 10000, \"attributes\": { \"app.name\": \"foo\", \"host.name\": \"dev.server.com\" } }, \"metrics\": [ { \"name\": \"service.errors.all\", \"type\": \"count\", \"value\": 9, \"attributes\": { \"service.response.statuscode\": \"400\" } }, { \"name\": \"service.errors.all\", \"type\": \"count\", \"value\": 4, \"attributes\": { \"service.response.statuscode\": \"500\" } }, { \"name\": \"service.response.duration\", \"type\": \"summary\", \"value\": { \"count\": 5, \"sum\": 0.004382655, \"min\": 0.0005093, \"max\": 0.001708826 }, \"attributes\": { \"service.response.statuscode\": \"200\" } } ] } ]";


  @Before
    public void init(){
        //sinkTask.mapper = mapper;
        configs = new HashMap<>();
        configs.put (TelemetrySinkConnectorConfig.API_KEY,"");
        configs.put(TelemetrySinkConnectorConfig.ACCOUNT_ID,"123");
        configs.put(TelemetrySinkConnectorConfig.MAX_RETRIES, "5");
        configs.put(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS, "1000");
    }
  @Test
  public void testPutMetricCount() throws ResponseException, JsonProcessingException {

    sinkTask.start(configs);
    sinkTask.sender = mock(MetricBatchSender.class);
    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  metricsList = mapper.readValue(metricCountJSON, List.class);

    records.add(new SinkRecord("test",0,null, null, null, metricsList, 0 ));
    when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

    sinkTask.put(records);
    assertEquals(1, sinkTask.metricBatch.size());

  }

  @Test
  public void testPutMetricCountGaugeSummary() throws ResponseException, JsonProcessingException {

    sinkTask.start(configs);
    sinkTask.sender = mock(MetricBatchSender.class);
    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  metricsList = mapper.readValue(metricCountGaugeSummaryJSON, List.class);

    records.add(new SinkRecord("test",0,null, null, null, metricsList, 0 ));
    when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

    sinkTask.put(records);
    assertEquals(3, sinkTask.metricBatch.size());

  }

  @Test
  public void testPutMetricWithCommons() throws ResponseException, JsonProcessingException {
    sinkTask.start(configs);
    sinkTask.sender = mock(MetricBatchSender.class);

    Collection<SinkRecord> records = new ArrayList<SinkRecord>();
    List<Map<String, Object>>  metricsList = mapper.readValue(metricCountGaugeSummaryJSON, List.class);

    records.add(new SinkRecord("test",0,null, null, null, metricsList, 0 ));
    when(sinkTask.sender.sendBatch(any(MetricBatch.class))).thenReturn(response);

    sinkTask.put(records);
    assertEquals(3, sinkTask.metricBatch.size());

  }
}
