package com.newrelic.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.events.models.EventModel;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Test;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRollupTransformationTest {

    @Test
    public void test() {
        final Map<String, Object> props = new HashMap<>();

        props.put("event.type", "CONCILIATION");
        props.put("timestamp.column", "TIMESTAMP");

        EventRollupTransformation<SourceRecord> eventRollupTransformation = new EventRollupTransformation<>();
        eventRollupTransformation.configure(props);

        final Schema simpleStructSchema = SchemaBuilder.struct().name("name").version(1).doc("doc").field("magic", Schema.OPTIONAL_INT64_SCHEMA).field("TIMESTAMP",Schema.INT64_SCHEMA).build();
        final Struct simpleStruct = new Struct(simpleStructSchema).put("magic", 42L).put("TIMESTAMP", 3000L);

        final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);

        SourceRecord outputRecord = eventRollupTransformation.apply(record);
        List<Object> models = (List<Object>) outputRecord.value();
        assert models.get(0).getClass().getSimpleName().equals("String");
        String output = ((String) models.get(0));
        assert  output.contains("CONCILIATION");



    }
}
