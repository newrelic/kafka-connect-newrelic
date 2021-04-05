package com.newrelic.telemetry.logs.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class LogModel {

    Map<String, Object> attributes = new HashMap<>();

    // Capture all fields
    @JsonAnyGetter
    public Map<String, Object> fields() {
        return attributes;
    }

    @JsonAnySetter
    public void setField(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public String toString() {
      return "LogModel{" + attributes + '}';
    }
}
