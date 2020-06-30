package com.newrelic.telemetry;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AgentRollupTransformation<R extends ConnectRecord<R>> implements Transformation<R> {
  private static Logger log = LoggerFactory.getLogger(AgentRollupTransformation.class);

  public static final String EVENT_TYPE = "event.type";

  public static final String AGENT_ROLLUP_KEY = "agent_rollup";
  public static final String CAPTURE_TIME_KEY = "capture_time";


  public static final String APP_NAME_KEY = "appName";
  public static final String APP_ID_KEY = "appID";
  public static final String SERVICE_KEY = "service";
  public static final String NODE_NAME_KEY = "nodeName";
  public static final String SVC_OR_POD_NAME_KEY = "svcOrPodName";
  public static final String TIMESTAMP_KEY = "timestamp";
  public static final String EVENT_TYPE_KEY = "eventType";
  // matches the pattern for a node name found in the agent_rollup field
  private static final Pattern NODE_PATTERN = Pattern.compile("[a-zA-Z]{3,4}[0-9]{3,5}");

  public static final ConfigDef CONFIG_DEF = new ConfigDef()
          .define(EVENT_TYPE, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                  "Event Type");

  private String eventType;

  @Override
  public R apply(R record) {
    try{
    final Map<String, Object> value = (Map<String, Object>) record.value();
    Map<String, Object> outputValue = new HashMap<>();
    outputValue.putAll(value);
    String agentRollup = (String)value.get(AGENT_ROLLUP_KEY);
    String[] values = agentRollup.replace("\"", "").split("::");
    if (values.length > 0) {
      String[] pieces = values[0].split("-", 3);

      if (pieces.length > 1) {
        String appName = pieces[0];
        String appId = pieces[1];
        String service = null;

        // all remainging peices become service name
        if (pieces.length > 2) {
          service = pieces[2];
        }

        outputValue.put(APP_NAME_KEY, appName);
        outputValue.put(APP_ID_KEY, appId);
        outputValue.put(SERVICE_KEY, service);
      }
    }
      if (values.length > 1) {
        String[] pieces = values[1].split("-");

        String nodeName = null;
        String svcPodName = null;

        if (pieces.length > 1) {
          // node in first field
          if (pieces[0].matches(NODE_PATTERN.pattern())) {
            nodeName = pieces[0];
            svcPodName = concatFields(pieces, 1, pieces.length);
          } else { // node in last field
            nodeName = pieces[pieces.length -1];
            svcPodName = concatFields(pieces, 0, pieces.length - 1);
          }
        }
        outputValue.put(NODE_NAME_KEY, nodeName);
        outputValue.put(SVC_OR_POD_NAME_KEY, svcPodName);

      }
      outputValue.put(TIMESTAMP_KEY,value.get(CAPTURE_TIME_KEY));
      outputValue.put(EVENT_TYPE_KEY, eventType);
    List<Map<String, Object>> outputValues = new ArrayList<>();
      outputValues.add(outputValue);
    return record.newRecord(
            record.topic(), record.kafkaPartition(),
            record.keySchema(), record.key(),
            record.valueSchema(), outputValues,
            record.timestamp()
    );
    } catch (Exception e) {
      log.error(e.getMessage());
      return null;
    }
  }

  private String concatFields(String[] elements, int start, int end) {
    StringBuilder sb = new StringBuilder();

    for (int i = start; i < end; i++) {
      sb.append(elements[i]);
      if (i < end - 1) {
        sb.append("-");
      }
    }
    return sb.toString();
  }

  @Override
  public ConfigDef config() {
    return CONFIG_DEF;
  }


  @Override
  public void close() {

  }

  @Override
  public void configure(Map<String, ?> props) {
    final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);

    eventType = config.getString(EVENT_TYPE);
  }
}