package com.newrelic.telemetry.metrics.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class MetricModel {
    @JsonProperty(required = true)
    public String name;
    @JsonProperty(required = true)
    public String type;
    @JsonProperty(required = true)
    public Long timestamp;
    public Map<String, Object> attributes;
}
