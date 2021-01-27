package com.newrelic.telemetry.metrics;

import com.newrelic.telemetry.events.EventsConverter;
import com.newrelic.telemetry.metrics.MetricsConverter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.junit.Test;

public class MetricsConverterTest {
    private static final String metricJSON = "[ { \"metrics\": [ { \"name\": \"cache.misses\", \"type\": \"count\", \"value\": 15, \"timestamp\": 1600458828505, \"interval.ms\": 10000, \"attributes\": { \"cache.name\": \"myCache\", \"host.name\": \"dev.server.com\" } }, { \"name\": \"temperature\", \"type\": \"gauge\", \"value\": 15, \"timestamp\": 1600458828505, \"attributes\": { \"city\": \"Portland\", \"state\": \"Oregon\" } }, { \"name\": \"service.response.duration\", \"type\": \"summary\", \"value\": { \"count\": 5, \"sum\": 0.004382655, \"min\": 0.0005093, \"max\": 0.001708826 }, \"interval.ms\": 10000, \"timestamp\": 1600458828505, \"attributes\": { \"host.name\": \"dev.server.com\", \"app.name\": \"foo\" } } ] } ]  ";
    private static final String metricJSONTSMissing = "[ { \"metrics\": [ { \"name\": \"cache.misses\", \"type\": \"count\", \"value\": 15, \"timestamp\": 1600458828505, \"interval.ms\": 10000, \"attributes\": { \"cache.name\": \"myCache\", \"host.name\": \"dev.server.com\" } }, { \"name\": \"temperature\", \"type\": \"gauge\", \"value\": 15,  \"attributes\": { \"city\": \"Portland\", \"state\": \"Oregon\" } }, { \"name\": \"service.response.duration\", \"type\": \"summary\", \"value\": { \"count\": 5, \"sum\": 0.004382655, \"min\": 0.0005093, \"max\": 0.001708826 }, \"interval.ms\": 10000, \"timestamp\": 1600458828505, \"attributes\": { \"host.name\": \"dev.server.com\", \"app.name\": \"foo\" } } ] } ]  ";

    @Test
    public void toConnectData() {
        SchemaAndValue schemaAndValue = new MetricsConverter().toConnectData("", metricJSON.getBytes());
        System.out.println(schemaAndValue.value());
    }

    @Test(expected = SerializationException.class)
    public void toConnectDataWithTimestampMissing() {
        SchemaAndValue schemaAndValue = new MetricsConverter().toConnectData("", metricJSONTSMissing.getBytes());
        System.out.println(schemaAndValue.value());

    }
}
