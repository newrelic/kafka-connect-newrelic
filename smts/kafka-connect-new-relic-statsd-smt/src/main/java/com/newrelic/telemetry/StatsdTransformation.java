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

public class StatsdTransformation<R extends ConnectRecord<R>> implements Transformation<R> {
    private static Logger log = LoggerFactory.getLogger(StatsdTransformation.class);

    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String TIMESTAMP = "timestamp";
    public static final String STATSD_METRIC = "metric";
    public static final String STATSD_TIMESTAMP = "ts";

    public static final ConfigDef CONFIG_DEF = new ConfigDef();

    private String eventType;

    @Override
    public R apply(R record) {
        Map<String, Object> statsdValue = (Map<String, Object>)record.value();


        Map<String, Object> metric = new HashMap<>();
        metric.put(VALUE, statsdValue.get(VALUE));
        metric.put(TYPE, "gauge");
        metric.put("name",statsdValue.get(STATSD_METRIC));
        metric.put(TIMESTAMP, statsdValue.get(STATSD_TIMESTAMP));

        Map<String, Object> metricAttributes = new HashMap<>();
        statsdValue.forEach((key, value)->{

            if(value instanceof Map) {
                Map<String, Object> attributes = (Map<String, Object>)value;
                attributes.forEach((attkey, attValue)->{
                    metricAttributes.put(key+":"+attkey, attValue);
                });
            } else
                metricAttributes.put(key, value );
        });
        metric.put("attributes",metricAttributes);
        List<Map<String, Object>> outputValues = new ArrayList<>();
        Map<String, Object> outputValue = new HashMap<>();
        List<Map<String, Object>> metricsList = new ArrayList<>();
        metricsList.add(metric);
        outputValue.put("metrics", metricsList);
        outputValues.add(outputValue);
        return record.newRecord(
                record.topic(), record.kafkaPartition(),
                record.keySchema(), record.key(),
                record.valueSchema(), outputValues,
                record.timestamp()
        );
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

    }
}