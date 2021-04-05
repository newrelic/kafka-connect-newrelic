package com.newrelic.telemetry.logs;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.logs.models.LogModel;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class LogsConverter implements Converter {
    private static Logger log = LoggerFactory.getLogger(LogsConverter.class);

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] fromConnectData(String topic, Schema schema, Object value) {
        List<LogModel> eventModels = (List<LogModel>)value;
        try {
            return new ObjectMapper().writeValueAsString(eventModels).getBytes();
        } catch (JsonProcessingException e) {
            log.error("Error while serializing events");
            return new byte[0];
        }

    }

    @Override
    public SchemaAndValue toConnectData(String topic, byte[] bytes) {
        try {
            String value = new String(bytes);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.setSerializationInclusion(Include.NON_NULL);
            List<LogModel> events = mapper.readValue(value, new TypeReference<List<LogModel>>() {
            });
            final SchemaAndValue obj = new SchemaAndValue(null, events);
            return (obj);
        }  catch (Exception e) {
            log.error("Error while deserializing events " + e.getMessage());
            throw new SerializationException(e.getMessage());
        }

    }
}
