package com.newrelic.telemetry;

import com.newrelic.telemetry.events.EventsConverter;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.junit.Test;

public class EventsConverterTest {
    private static final String eventJSON ="[{\"eventType\": \"SystemSample\",\"diskTotalBytes\": 11000000000,\"timestamp\": 1593563273326,\"awsRegion\": \"eu-central-1\",\"hostName\": \"colins-machine\"}]";
    @Test
    public void toConnectData(){
        SchemaAndValue schemaAndValue = new EventsConverter().toConnectData("", eventJSON.getBytes() );
        System.out.println(schemaAndValue.value());
    }
}
