# Introduction

Welcome to your new Kafka Connect Event Rollup SMT!

# How to run

- Build the zip using `mvn clean package` and unpack the zip into the kafka connect plugins directory
- This SMT works with the Events Sink connector
- Use the following json sample to post a job with this SMT 
    ```
    {
  "name": "agent-rollup-connector",
  "config": {
        "connector.class": "com.newrelic.telemetry.TelemetryEventsSinkConnector",
        "value.converter": "io.confluent.connect.avro.AvroConverter",
        "topics": "events_sample",
        "api.key": [API_KEY],
        "transforms":"eventRollup",
        "transforms.eventRollup.type":"com.newrelic.telemetry.EventRollupTransformation",
        "transforms.eventRollup.event.type":"CONFLUENT_EVENT_SAMPLE"
        }
    }
    ```
- This will allow you to post a message to new relic from events_sample topic.
- After you post the message head over to insights and a new event of type `CONFLUENT_EVENT_SAMPLE` will be created.
