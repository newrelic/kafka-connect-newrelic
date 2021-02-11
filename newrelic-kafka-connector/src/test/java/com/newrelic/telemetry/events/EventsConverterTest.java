package com.newrelic.telemetry.events;

import com.newrelic.telemetry.events.EventsConverter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.junit.Test;

public class EventsConverterTest {
    private static final String eventJSON = "[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"timestamp\": 1593563273326,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";
    private static final String eventJSONTSMissing = "[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";

    @Test
    public void toConnectData() {
        SchemaAndValue schemaAndValue = new EventsConverter().toConnectData("", eventJSON.getBytes());
        System.out.println(schemaAndValue.value());
    }

    @Test(expected = SerializationException.class)
    public void toConnectDataWithTimestampMissing() {
        SchemaAndValue schemaAndValue = new EventsConverter().toConnectData("", eventJSONTSMissing.getBytes());
        System.out.println(schemaAndValue.value());

    }
}
