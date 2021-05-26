package com.newrelic.telemetry.logs;

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
 * utilities used to convert a single record to a New Relic Log
 */
public class LogConverter {

    public static final String LOG_MESSAGE = "logMessage";
    public static final String PARTITION_NAME = "partitionName";
    public static final String LEVEL = "logLevel";

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

        String logLevel = "";
        Optional<Field> loglevelField = schema.fields().stream().filter(f -> f.name().equals(LEVEL)).findAny();
        if (loglevelField.isPresent()) {
            logLevel = value.getString(LEVEL);
        }


        Attributes attributes = new Attributes();

        // add fields from the record
        schema.fields().stream()
                .filter(f -> !f.name().equals(LOG_MESSAGE))
                .forEach(f -> {
                    switch(f.schema().type()){
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

        // create the log using the record's timestamp
        Log newlog = Log.builder()
        .attributes(attributes)
        .message(logMessage)
        .level(logLevel)
        .build();

        return newlog;

    }

    private static Log withoutSchema(SinkRecord record) {
        // check if the value in the record is a Map.  If not, throw https://kafka.apache.org/24/javadoc/org/apache/kafka/connect/errors/DataException.html
        // then convert the map

	    if (!(record.value() instanceof Map)) {
            throw new DataException("value must be instance of Map");
        }

        Map recordMapValue = (Map)record.value();

        // String eventType = "";

        // if (!recordMapValue.containsKey(EVENT_TYPE_ATTRIBUTE)) {
        //     throw new DataException(String.format("All records must contain a '%s' field", EVENT_TYPE_ATTRIBUTE));
        // } else {
        //     eventType = recordMapValue.get(EVENT_TYPE_ATTRIBUTE).toString();
        // }

        // Attributes attributes = new Attributes();

        // Set<Map.Entry<String, Object>> entries = recordMapValue.entrySet();
        // entries.stream()
        //     .filter(e -> !e.getKey().equals(EVENT_TYPE_ATTRIBUTE))
        //     .forEach(e -> {

        //         String key = e.getKey().toString();

        //         if (e.getValue() instanceof String) {

        //             attributes.put(key, new String(e.getValue().toString()));

        //         } else if (e.getValue() instanceof Number) {

        //             if (e.getValue() instanceof Float) {
        //                 attributes.put(key, Float.valueOf(Float.parseFloat(e.getValue().toString())));
        //             }
        //             else if (e.getValue() instanceof Integer) {
        //                 attributes.put(key, Integer.valueOf(Integer.parseInt(e.getValue().toString())));
        //             }

        //             // else if (e.getValue() instanceof Double) {
        //             //     attributes.put(key, Double.valueOf(Double.parseDouble(e.getValue().toString())));
        //             // }

        //             else {
        //                 // handle all other cases as strings
        //                 attributes.put(key, new String(e.getValue().toString()));
        //             }

        //         } else {
        //             System.out.println("not writing attribute for: " + e.getKey().toString());
        //         }

        //     });

        // // create the event using the record's timestamp
        // //Event event = new Event(eventType, attributes, record.timestamp());
        // Event event = new Event(eventType, attributes, record.timestamp());

        // return event;
        return null;

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
        attributes.put("metadata.kafkaTopic", record.topic());
        attributes.put("metadata.kafkaPartition", String.valueOf(record.kafkaPartition()));
        attributes.put("metadata.kafkaOffset", record.kafkaOffset());

        return log;
    }

}
