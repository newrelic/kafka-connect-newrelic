package com.newrelic.telemetry.metrics;

import com.newrelic.telemetry.Attributes;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;


import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.stream.*;

/**
 * utilities used to convert a single record to a New Relic metric
 */
public class MetricConverter {

    public static final String METRIC_NAME = "name";

    //// gauge example (from prometheus -> vector -> kafka):
    // {"name":"prometheus_tsdb_wal_segment_current","timestamp":"2021-06-05T21:30:41.740190293Z","kind":"absolute","gauge":{"value":1.0}}
    // therefore values are gauge.value, counter.value, 
    public static final String METRIC_VALUE_GAUGE = "gauge.value";
    public static final String METRIC_VALUE_COUNTER = "counter.value";
    
    // not sure yet about summaries...
    public static final String SUMMARY_METRIC_COUNT = "aggregated_summary.count";
    public static final String SUMMARY_METRIC_SUM = "aggregated_summary.sum";
    public static final String SUMMARY_METRIC_MIN = "aggregated_summary.min";
    public static final String SUMMARY_METRIC_MAX = "aggregated_summary.max";

    public static final String TIMESTAMP_ATTRIBUTE = "timestamp";

    private static Metric withSchema(SinkRecord record) {

        if (!(record.value() instanceof Struct)) {
            throw new DataException("Can only operate on instances of Struct");
        }

        final Struct value = (Struct) record.value();
        Schema schema = record.valueSchema();

        // start with attributes
        Attributes attributes = new Attributes();
        attributes.put("metadata.kafkaTopic", record.topic());
        attributes.put("metadata.kafkaPartition", String.valueOf(record.kafkaPartition()));
        attributes.put("metadata.kafkaOffset", record.kafkaOffset());

        // if the inbound data not in the filter list here, tread it as an attribute - i.e. will become a metric dimension
        schema.fields().stream()
        .filter(f -> !(f.name().equals(METRIC_NAME)
            || f.name().equals(METRIC_VALUE_GAUGE)
            || f.name().equals(METRIC_VALUE_COUNTER)
            || f.name().equals(SUMMARY_METRIC_COUNT)
            || f.name().equals(SUMMARY_METRIC_SUM)
            || f.name().equals(SUMMARY_METRIC_MIN)
            || f.name().equals(SUMMARY_METRIC_MAX)
            || f.name().equals(TIMESTAMP_ATTRIBUTE)))
        .forEach(f -> {
            switch (f.schema().type()) {
                case BOOLEAN:
                    attributes.put(f.name(), value.getBoolean(f.name()));
                    break;
                case FLOAT32:
                    attributes.put(f.name(), value.getFloat32(f.name()));
                    break;
                case FLOAT64:
                    attributes.put(f.name(), value.getFloat64(f.name()));
                    break;
                case INT16:
                    attributes.put(f.name(), value.getInt16(f.name()));
                    break;
                case INT32:
                    attributes.put(f.name(), value.getInt32(f.name()));
                    break;
                case INT64:
                    attributes.put(f.name(), value.getInt64(f.name()));
                    break;
                case INT8:
                    attributes.put(f.name(), value.getInt8(f.name()));
                    break;
                default:
                    attributes.put(f.name(), value.getString(f.name()));
                    break;
            }
        });

        // all metrics must have name field
        String metricName = "";
        Optional<Field> metricNameField = schema.fields().stream().filter(
            f -> f.name().equals(METRIC_NAME)).findAny();
        if (!metricNameField.isPresent()) {
            throw new DataException(String.format("All records must contain a '%s' field", metricNameField));
        } else {
            metricName = value.getString(METRIC_NAME);
        }

        // timestamp may or may not be on the record
        long timestamp;
        Optional<Field> timestampField = schema.fields().stream().filter(
            f -> f.name().equals(TIMESTAMP_ATTRIBUTE)).findAny();
        if (timestampField.isPresent()) {
            timestamp = value.getInt64(TIMESTAMP_ATTRIBUTE);
        } else {
            timestamp = java.lang.System.currentTimeMillis();
        }

        // these will determine which type of metric this is
        Optional<Field> gaugeMetricValueField = schema.fields().stream().filter(f -> f.name().equals(METRIC_VALUE_GAUGE)).findAny();
        Optional<Field> counterMetricValueField = schema.fields().stream().filter(f -> f.name().equals(METRIC_VALUE_COUNTER)).findAny();    

        // for summary metrics:
        Optional<Field> summaryMetricCountField = schema.fields().stream().filter(f -> f.name().equals(SUMMARY_METRIC_COUNT)).findAny();
        Optional<Field> summaryMetricSumField = schema.fields().stream().filter(f -> f.name().equals(SUMMARY_METRIC_SUM)).findAny();
        Optional<Field> summaryMetricMinField = schema.fields().stream().filter(f -> f.name().equals(SUMMARY_METRIC_MIN)).findAny();
        Optional<Field> summaryMetricMaxField = schema.fields().stream().filter(f -> f.name().equals(SUMMARY_METRIC_MAX)).findAny();

        // TODO remove "= null" here, handle properly
        Metric metric = null;

        // gauge and counter will have 'value'; summary will not.
        double metricValue;

        // branch on metric type here; create appropriate telemetry
        if (gaugeMetricValueField.isPresent()) {
            metricValue = value.getFloat64(METRIC_VALUE_GAUGE);
            metric = new Gauge(metricName, metricValue, timestamp, attributes);
        } else if (counterMetricValueField.isPresent()) {
            metricValue = value.getFloat64(METRIC_VALUE_COUNTER);
            metric = new Count(metricName, metricValue, timestamp, timestamp, attributes);
        }
        else if (
            summaryMetricCountField.isPresent() 
            || summaryMetricSumField.isPresent()
            || summaryMetricMinField.isPresent()
            || summaryMetricMaxField.isPresent()
        ) {
            int count = 0;
            double sum = 0;
            double min = 0;
            double max = 0;
            if (summaryMetricCountField.isPresent()) {
                count = value.getInt32(SUMMARY_METRIC_COUNT);
            }
            if (summaryMetricSumField.isPresent()) {
                sum = value.getFloat64(SUMMARY_METRIC_SUM);
            }
            if (summaryMetricMinField.isPresent()) {
                min = value.getFloat64(SUMMARY_METRIC_MIN);
            }
            if (summaryMetricMaxField.isPresent()) {
                max = value.getFloat64(SUMMARY_METRIC_MAX);
            }
            metric = new Summary(metricName, count, sum, min, max, timestamp, timestamp, attributes);
        }

        if (null == metric) {
            throw new DataException(String.format("withSchema: unable to create metric")); // type was '%s'", metricType));
        }
        return metric;
    }

    private static Metric withoutSchema(SinkRecord record) {
        // check if the value in the record is a Map.  If not, throw https://kafka.apache.org/24/javadoc/org/apache/kafka/connect/errors/DataException.html
        // then convert the map to an EventModel

    	if (!(record.value() instanceof Map)) {
            throw new DataException("value must be instance of Map");
        }

        Map recordMapValue = (Map)record.value();

        Attributes attributes = new Attributes();
        attributes.put("metadata.kafkaTopic", record.topic());
        attributes.put("metadata.kafkaPartition", String.valueOf(record.kafkaPartition()));
        attributes.put("metadata.kafkaOffset", record.kafkaOffset());

        Set<Map.Entry<String, Object>> entries = recordMapValue.entrySet();
        entries.stream()
                .filter(e -> !(e.getKey().equals(METRIC_NAME) 
                || e.getKey().equals(METRIC_VALUE_GAUGE)
                || e.getKey().equals(METRIC_VALUE_COUNTER)
                || e.getKey().equals(SUMMARY_METRIC_COUNT)
                || e.getKey().equals(SUMMARY_METRIC_SUM)
                || e.getKey().equals(SUMMARY_METRIC_MIN)
                || e.getKey().equals(SUMMARY_METRIC_MAX)
                || e.getKey().equals(TIMESTAMP_ATTRIBUTE)
                ))
                .forEach(m -> {
                    String key = m.getKey();
                    if (m.getValue() instanceof String) {
                        attributes.put(key, (String) m.getValue());
                    } else if (m.getValue() instanceof Number) {
                        if (m.getValue() instanceof Float) {
                            attributes.put(key, (Float) m.getValue());
                        } else if (m.getValue() instanceof Integer) {
                            attributes.put(key, (Integer) m.getValue());
                        } else {
                            // handle all other cases as strings
                            attributes.put(key, new String(m.getValue().toString()));
                        }
                    } else {
                        System.out.println("Metric Converter: not writing attribute for: " + m.getKey().toString());
                    }
                });


        String metricName = "";
        if (!recordMapValue.containsKey(METRIC_NAME)) {
            throw new DataException(String.format("All metric records must contain a '%s' field", METRIC_NAME));
        } else {
            metricName = recordMapValue.get(METRIC_NAME).toString();
        }

        long timestamp;
        if (recordMapValue.containsKey(TIMESTAMP_ATTRIBUTE)) {
            timestamp = Long.valueOf(recordMapValue.get(TIMESTAMP_ATTRIBUTE).toString()).longValue();
        } else {
            timestamp = java.lang.System.currentTimeMillis();
        }


        // TODO remove "= null" here, handle properly
        Metric metric = null;

        double metricValue;
        if (recordMapValue.containsKey(METRIC_VALUE_GAUGE)) {
            metricValue = Double.valueOf(recordMapValue.get(METRIC_VALUE_GAUGE).toString()).doubleValue();
            metric = new Gauge(metricName, metricValue, timestamp, attributes);
        } else if (recordMapValue.containsKey(METRIC_VALUE_COUNTER)) {
            metricValue = Double.valueOf(recordMapValue.get(METRIC_VALUE_COUNTER).toString()).doubleValue();
            metric = new Count(metricName, metricValue, timestamp, timestamp, attributes);
        }
        else if (
            recordMapValue.containsKey(SUMMARY_METRIC_COUNT)
            || recordMapValue.containsKey(SUMMARY_METRIC_SUM)
            || recordMapValue.containsKey(SUMMARY_METRIC_MIN)
            || recordMapValue.containsKey(SUMMARY_METRIC_MAX)
        ) {

            int count = 0;
            double sum = 0;
            double min = 0;
            double max = 0;

            if (recordMapValue.containsKey(SUMMARY_METRIC_COUNT)){
                count = Integer.valueOf(recordMapValue.get(SUMMARY_METRIC_COUNT).toString()).intValue();
            }
            if (recordMapValue.containsKey(SUMMARY_METRIC_SUM)){
                sum = Double.valueOf(recordMapValue.get(SUMMARY_METRIC_SUM).toString()).doubleValue();
            }
            if (recordMapValue.containsKey(SUMMARY_METRIC_MIN)){
                min = Double.valueOf(recordMapValue.get(SUMMARY_METRIC_MIN).toString()).doubleValue();
            }
            if (recordMapValue.containsKey(SUMMARY_METRIC_MAX)){
                max = Double.valueOf(recordMapValue.get(SUMMARY_METRIC_MAX).toString()).doubleValue();
            }
            metric = new Summary(metricName, count, sum, min, max, timestamp, timestamp, attributes);
        }

        if (null == metric) {
            throw new DataException(String.format("withoutSchema: unable to create metric")); // type was '%s'", metricType));
        }
        return metric;
    }

    public static Metric toNewRelicMetric(SinkRecord record) {
        Metric metric;
        if (record.valueSchema() == null) {
            metric = withoutSchema(record);
        } else {
           metric = withSchema(record);
        }
        return metric;
    }
}
