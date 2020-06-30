package com.newrelic.telemetry.metrics.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SummaryValueModel {
    @JsonProperty(required = true)
    public int count;
    @JsonProperty(required = true)
    public Double sum;
    @JsonProperty(required = true)
    public Double min;
    @JsonProperty(required = true)
    public Double max;

    @JsonCreator
    public SummaryValueModel(@JsonProperty( value = "count", required = true) int _count,
                             @JsonProperty(value = "sum", required = true) Double _sum,
                             @JsonProperty(value = "min", required = true) Double _min,
                             @JsonProperty(value = "max", required = true) Double _max) {
        this.count = _count;
        this.sum = _sum;
        this.min = _min;
        this.max = _max;
    }
}
