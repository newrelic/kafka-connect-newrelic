package com.newrelic.telemetry.events;

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
 * utilities used to convert a single record to a New Relic Event
 */
public class EventConverter {

    public static final String EVENT_TYPE_ATTRIBUTE = "eventType";

    public static final String TIMESTAMP_ATTRIBUTE = "timestamp";

    private static Event withSchema(SinkRecord record) {

        if (!(record.value() instanceof Struct)) {
            throw new DataException("Can only operate on instances of Struct");
        }

        final Struct value = (Struct) record.value();

        Schema schema = record.valueSchema();
        Optional<Field> eventType = schema.fields().stream().filter(f -> f.name().equals(EVENT_TYPE_ATTRIBUTE)).findAny();
        if (!eventType.isPresent()) {
            throw new DataException(String.format("All records must contain a '%s' field", EVENT_TYPE_ATTRIBUTE));
        }

        Attributes attributes = new Attributes();

        // add fields from the record
        schema.fields().stream()
                .filter(f -> !(f.name().equals(EVENT_TYPE_ATTRIBUTE) || f.name().equals(TIMESTAMP_ATTRIBUTE)))
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

        Event event;
        if (schema.field(TIMESTAMP_ATTRIBUTE) != null) {
            event = new Event(value.getString(EVENT_TYPE_ATTRIBUTE), attributes, value.getInt64(TIMESTAMP_ATTRIBUTE));
        } else {
            event = new Event(value.getString(EVENT_TYPE_ATTRIBUTE), attributes);
        }

        return event;

    }

    private static Event withoutSchema(SinkRecord record) {
        // check if the value in the record is a Map.  If not, throw https://kafka.apache.org/24/javadoc/org/apache/kafka/connect/errors/DataException.html
        // then convert the map to an EventModel

	if (!(record.value() instanceof Map)) {
            throw new DataException("value must be instance of Map");
        }

        Map recordMapValue = (Map)record.value();

        if (!recordMapValue.containsKey(EVENT_TYPE_ATTRIBUTE)) {
            throw new DataException(String.format("All records must contain a '%s' field", EVENT_TYPE_ATTRIBUTE));
        }

        Attributes attributes = new Attributes();

        Set<Map.Entry<String, Object>> entries = recordMapValue.entrySet();
        entries.stream()
            .filter(e -> !(e.getKey().equals(EVENT_TYPE_ATTRIBUTE) || e.getKey().equals(TIMESTAMP_ATTRIBUTE)))
            .forEach(e -> {

                String key = e.getKey().toString();

                if (e.getValue() instanceof String) {

                    attributes.put(key, new String(e.getValue().toString()));

                } else if (e.getValue() instanceof Number) {

                    if (e.getValue() instanceof Float) {
                        attributes.put(key, Float.valueOf(Float.parseFloat(e.getValue().toString())));
                    }
                    else if (e.getValue() instanceof Integer) {
                        attributes.put(key, Integer.valueOf(Integer.parseInt(e.getValue().toString())));
                    }

                    // else if (e.getValue() instanceof Double) {
                    //     attributes.put(key, Double.valueOf(Double.parseDouble(e.getValue().toString())));
                    // }

                    else {
                        // handle all other cases as strings
                        attributes.put(key, new String(e.getValue().toString()));
                    }

                } else {
                    System.out.println("Event Converter: not writing attribute for: " + e.getKey().toString());
                }

            });

        Event event;
        if (recordMapValue.containsKey(TIMESTAMP_ATTRIBUTE)) {
            event = new Event((String) recordMapValue.get(EVENT_TYPE_ATTRIBUTE), attributes, (Long) recordMapValue.get(TIMESTAMP_ATTRIBUTE));
        } else {
            event = new Event((String) recordMapValue.get(EVENT_TYPE_ATTRIBUTE), attributes);
        }

        return event;

    }


    public static Event toNewRelicEvent(SinkRecord record) {
        Event event;
        if (record.valueSchema() == null) {
            event = withoutSchema(record);
        } else {
           event = withSchema(record);
        }

        Attributes attributes = event.getAttributes();

        // add kafka metadata fields.
        attributes.putAll(KafkaMetadata.getAttributes(record));

        return event;
    }

}
