package com.newrelic.telemetry;

import org.apache.kafka.connect.sink.SinkRecord;

public class KafkaMetadata {

    public static Attributes getAttributes(SinkRecord record) {
        Attributes attributes = new Attributes();
        attributes.put("instrumentation.metadata.kafka.topic", record.topic());
        attributes.put("instrumentation.metadata.kafka.partition", String.valueOf(record.kafkaPartition()));
        attributes.put("instrumentation.metadata.kafka.offset", record.kafkaOffset());
        return attributes;
    }
}
