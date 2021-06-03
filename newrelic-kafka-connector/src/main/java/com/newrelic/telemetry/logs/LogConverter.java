package com.newrelic.telemetry.logs;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.KafkaMetadata;
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
 * utilities used to convert a single record to a New Relic Log
 */
public class LogConverter {

    public static final String LOG_MESSAGE = "message";
    public static final String TIMESTAMP_ATTRIBUTE = "timestamp";

    private static Log withSchema(SinkRecord record) {

        if (!(record.value() instanceof Struct)) {
            throw new DataException("Can only operate on instances of Struct");
        }

        final Struct value = (Struct) record.value();

        Schema schema = record.valueSchema();

        String logMessage = "";
        Optional<Field> logMessageField = schema.fields().stream().filter(f -> f.name().equals(LOG_MESSAGE)).findAny();
        if (!logMessageField.isPresent()) {
            throw new DataException(String.format("All records must contain a '%s' field", LOG_MESSAGE));
        } else {
            logMessage = value.getString(LOG_MESSAGE);
        }

        Attributes attributes = new Attributes();

        // add fields from the record
        schema.fields().stream()
                .filter(f -> !(f.name().equals(LOG_MESSAGE) || f.name().equals(TIMESTAMP_ATTRIBUTE)))
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


        Log.LogBuilder builder = Log.builder()
                .message(logMessage)
                .attributes(attributes);

        if (schema.field(TIMESTAMP_ATTRIBUTE) != null) {
            builder.timestamp(value.getInt64(TIMESTAMP_ATTRIBUTE));
        }

        return builder.build();

    }

    private static Log withoutSchema(SinkRecord record) {
        // check if the value in the record is a Map.  If not, throw https://kafka.apache.org/24/javadoc/org/apache/kafka/connect/errors/DataException.html
        // then convert the map

        if (!(record.value() instanceof Map)) {
            throw new DataException("value must be instance of Map");
        }

        Map recordMapValue = (Map) record.value();
        Attributes attributes = new Attributes();

        String logMessage = "";
        if (!recordMapValue.containsKey(LOG_MESSAGE)) {
            throw new DataException(String.format("All records must contain a '%s' field", LOG_MESSAGE));
        } else {
            logMessage = recordMapValue.get(LOG_MESSAGE).toString();
        }

        Set<Map.Entry<String, Object>> entries = recordMapValue.entrySet();
        entries.stream()
                .filter(e -> !(e.getKey().equals(LOG_MESSAGE) || e.getKey().equals(TIMESTAMP_ATTRIBUTE)))
                .forEach(l -> {
                    String key = l.getKey();
                    if (l.getValue() instanceof String) {
                        attributes.put(key, (String) l.getValue());
                    } else if (l.getValue() instanceof Number) {
                        if (l.getValue() instanceof Float) {
                            attributes.put(key, (Float) l.getValue());
                        } else if (l.getValue() instanceof Integer) {
                            attributes.put(key, (Integer) l.getValue());
                        } else {
                            // handle all other cases as strings
                            attributes.put(key, new String(l.getValue().toString()));
                        }
                    } else {
                        System.out.println("not writing attribute for: " + l.getKey().toString());
                    }
                });

        Log.LogBuilder builder = Log.builder()
                .message(logMessage)
                .attributes(attributes);

        if (recordMapValue.containsKey(TIMESTAMP_ATTRIBUTE)) {
            builder.timestamp((Long) recordMapValue.get(TIMESTAMP_ATTRIBUTE));
        }

        return builder.build();

    }


    public static Log toNewRelicLog(SinkRecord record) {
        Log log;
        if (record.valueSchema() == null) {
            log = withoutSchema(record);
        } else {
            log = withSchema(record);
        }

        Attributes attributes = log.getAttributes();

        // add kafka metadata fields.
        attributes.putAll(KafkaMetadata.getAttributes(record));

        return log;
    }

}
