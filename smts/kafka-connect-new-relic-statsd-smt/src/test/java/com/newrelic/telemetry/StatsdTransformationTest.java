package com.newrelic.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;
public class StatsdTransformationTest {
  @Test
  public void test() throws IOException {
    StatsdTransformation<SourceRecord> statsdTransformation = new StatsdTransformation<>();
    ObjectMapper mapper = new ObjectMapper();
    String statsdJson ="{\"eo\":\"argos-agent\",\"group\":\"statsd_metrics\",\"tags\":{\"TeamEmail\":\"test@test.com\", \"Datadog\":false, \"Name\":\"rahul\", \"ProcessesName\":\"argos-duid\"}, \"env_group\":\"ecomqc\", \"metric\":\"heap\", \"pid\":0, \"ts\":1593449922065, \"value\":4223, \"s4\":\"10.51.11.119\", \"env\":\"master\", \"av\":\"0.0.1\", \"type\":\"Gauges\"}";
    Map<String, String> map = mapper.readValue(statsdJson, Map.class);



    SourceRecord record = new SourceRecord(
            null, null,
            "test", 0,
            null, null,
            null, map,
            1483425001864L
    );

    SourceRecord outputRecord =statsdTransformation.apply(record);
    System.out.println(outputRecord.value());
  }
}
