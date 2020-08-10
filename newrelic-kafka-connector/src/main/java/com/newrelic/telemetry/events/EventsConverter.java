package com.newrelic.telemetry.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.events.models.EventModel;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class EventsConverter implements Converter {
    private static Logger log = LoggerFactory.getLogger(EventsConverter.class);

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] fromConnectData(String s, Schema schema, Object o) {
        List<EventModel> eventModels = (List<EventModel>) o;
        try {
            return new ObjectMapper().writeValueAsString(eventModels).getBytes();
        } catch (JsonProcessingException e) {
            log.error("Error while serializing events");
            return new byte[0];
        }

    }

    @Override
    public SchemaAndValue toConnectData(String s, byte[] bytes) {
        try {
            List<EventModel> events = new ObjectMapper().readValue(new String(bytes), new TypeReference<List<EventModel>>() {
            });
            return (new SchemaAndValue(null, events));
        } catch (JsonProcessingException e) {
            log.error("Error while deserializing events " + e.getMessage());
        }
        return null;
    }
}
