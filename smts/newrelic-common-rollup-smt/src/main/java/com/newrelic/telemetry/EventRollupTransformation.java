package com.newrelic.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.events.models.EventModel;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventRollupTransformation<R extends ConnectRecord<R>> implements Transformation<R> {
    private static Logger log = LoggerFactory.getLogger(EventRollupTransformation.class);

    public static final String EVENT_TYPE = "event.type";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(EVENT_TYPE, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                    "Event Type");

    private String eventType;
    private String timestampColumn;


    @Override
    public R apply(R record) {
        try {
            if (record.value() == null) {
                return record;
            }
            Struct originalValueStruct = (Struct) record.value();
            EventModel eventModel = new EventModel(eventType,  System.currentTimeMillis());
            for (Field field : originalValueStruct.schema().fields()) {
                eventModel.setOtherField(field.name(), originalValueStruct.get(field));
            }
            log.debug(new ObjectMapper().writeValueAsString(eventModel));
            List<String> outputValues = new ArrayList<>();
            outputValues.add(new ObjectMapper().writeValueAsString(eventModel));

            return record.newRecord(
                    record.topic(), record.kafkaPartition(),
                    record.keySchema(), record.key(),
                    null,  outputValues,
                    record.timestamp()
            );

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
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

        eventType = config.getString(EVENT_TYPE);

    }
}
