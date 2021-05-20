package com.newrelic.telemetry.events;

import com.newrelic.telemetry.Attributes;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;


import java.util.Optional;

/**
 * utilities used to convert a single record to a New Relic Event
 */
public class EventConverter {

    public static final String EVENT_TYPE_ATTRIBUTE = "eventType";

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
                .filter(f -> !f.name().equals(EVENT_TYPE_ATTRIBUTE))
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

        // create the event using the record's timestamp
        Event event = new Event(value.getString(EVENT_TYPE_ATTRIBUTE), attributes, record.timestamp());

        return event;

    }

    private static Event withoutSchema(SinkRecord record) {
        // check if the value in the record is a Map.  If not, throw https://kafka.apache.org/24/javadoc/org/apache/kafka/connect/errors/DataException.html
        // then convert the map to an EventModel

        return null;
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
        attributes.put("metadata.kafkaTopic", record.topic());
        attributes.put("metadata.kafkaPartition", String.valueOf(record.kafkaPartition()));
        attributes.put("metadata.kafkaOffset", record.kafkaOffset());

        return event;
    }

}
