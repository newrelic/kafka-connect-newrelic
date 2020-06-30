package com.newrelic.telemetry;

import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AgentRollupTransformationTest {
  @Test
  public void test() {
    AgentRollupTransformation<SourceRecord> agentRollupTransformation = new AgentRollupTransformation<>();
    Map<String, Object> valuesMap = new HashMap<>();
    valuesMap.put(AgentRollupTransformation.AGENT_ROLLUP_KEY, "FASTBPM-27267-OrderProvisioning-ATLAS::zlp34997-orderprovisioning-1-4-atlas-6fb7544d9b-9mh2w");
    valuesMap.put( "gauge_name","java.lang:type=MemoryPool,name=Compressed Class Space:Usage.used");
    valuesMap.put("capture_time", 1588169400000l);
    valuesMap.put("value", 1.749964E7);
    valuesMap.put("weight", 60);

    SourceRecord record = new SourceRecord(
            null, null,
            "test", 0,
            null, null,
            null, valuesMap,
            1483425001864L
    );

    SourceRecord outputRecord =agentRollupTransformation.apply(record);
    System.out.println(outputRecord.value());
  }
}