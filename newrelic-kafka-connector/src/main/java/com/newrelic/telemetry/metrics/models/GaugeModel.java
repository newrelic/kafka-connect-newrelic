package com.newrelic.telemetry.metrics.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GaugeModel extends MetricModel {
    @JsonProperty(required = true)
    public Double value;


    @JsonCreator
    public GaugeModel(@JsonProperty(value = "name", required = true) String _name,
                      @JsonProperty(value = "type", required = true) String _type,
                      @JsonProperty(value = "timestamp", required = true) Long _timestamp,
                      @JsonProperty(value = "value", required = true) Double _value) {
        this.name = _name;
        this.value = _value;
        this.type = _type;
        this.timestamp = _timestamp;

    }
}
