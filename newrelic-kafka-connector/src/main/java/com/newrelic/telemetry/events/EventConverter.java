package com.newrelic.telemetry.events;

import com.newrelic.telemetry.events.models.EventModel;
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

    public static EventModel withSchema(SinkRecord record) {
        Schema schema = record.valueSchema();
        if (schema == null) {
            throw new DataException("Method 'withSchema' called with record containing a null schema");
        }

        if (!(record.value() instanceof Struct)) {
            throw new DataException("Can only operate on instances of Struct");
        }

        final Struct value = (Struct) record.value();

        Optional<Field> eventType = schema.fields().stream().filter(f -> f.name().equals(EVENT_TYPE_ATTRIBUTE)).findAny();
        if (!eventType.isPresent()) {
            throw new DataException(String.format("All records must contain a '%s' field", EVENT_TYPE_ATTRIBUTE));
        }

        // create the event using the record's timestamp
        EventModel event = new EventModel(value.getString(EVENT_TYPE_ATTRIBUTE), record.timestamp());

        // add some additional metadata fields.
        event.setOtherField("metadata.kafkaTopic", record.topic());
        event.setOtherField("metadata.kafkaPartition", String.valueOf(record.kafkaPartition()));
        event.setOtherField("metadata.kafkaOffset", record.kafkaOffset());

        // now add the remaining fields from the record
        schema.fields().stream()
                .filter(f -> !f.name().equals(EVENT_TYPE_ATTRIBUTE))
                .forEach(f -> event.setOtherField(f.name(), value.get(f)));

        return event;

    }

    public static EventModel withoutSchema(SinkRecord record) {
        // check if the value in the record is a Map.  If not, throw https://kafka.apache.org/24/javadoc/org/apache/kafka/connect/errors/DataException.html
        // then convert the map to an EventModel

        return null;
    }

}
