package com.newrelic.telemetry;

import com.newrelic.telemetry.logs.LogsConverter;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.junit.Test;

public class LogsConverterTest {
    private static final String logsJSON =            "{ \"key1\": \"value1\", \"key2\": \"value2\", \"key3\": \"value3\"}";
    private static final String logsJSONNestedValue = "{ \"key1\": \"value1\", \"key2\": \"value2\", \"key3\": \"value3\", \"nestedkey\": {\"key1\": \"value4\",\"key2\": \"value5\"}}";

    @Test
    public void toConnectData() {
        SchemaAndValue schemaAndValue = new LogsConverter().toConnectData("", logsJSON.getBytes());
        System.out.println(schemaAndValue.value());
    }

    @Test
    public void toConnectDataWithNestedValues() {
        SchemaAndValue schemaAndValue = new LogsConverter().toConnectData("", logsJSONNestedValue.getBytes());
        System.out.println(schemaAndValue.value());

    }    
}
