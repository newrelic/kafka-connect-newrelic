package com.newrelic.telemetry.events.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class EventModel {
    @JsonProperty(required = true)
    public String eventType;

    @JsonProperty(required = true)
    public long timestamp;

    @JsonCreator
    public EventModel(@JsonProperty(value = "eventType", required = true) String _eventType,
                      @JsonProperty(value = "timestamp", required = true) Long _timeStamp) {
        this.eventType = _eventType;
        this.timestamp = _timeStamp;
    }

    Map<String, Object> attributes = new HashMap<>();

    // Capture all other fields that Jackson do not match other members
    @JsonAnyGetter
    public Map<String, Object> otherFields() {
        return attributes;
    }

    @JsonAnySetter
    public void setOtherField(String name, Object value) {
        attributes.put(name, value);
    }


}