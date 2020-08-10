# Introduction

Welcome to your new Kafka Connect Agent Rollup SMT!

# How to run

- Build the zip using `mvn clean package` and unpack the zip into the kafka connect plugins directory
- This SMT works with the Events Sink connector
- Use the following json sample to post a job with this SMT 
    ```
    {
  "name": "agent-rollup-connector",
  "config": {
        "connector.class": "com.newrelic.telemetry.TelemetryEventsSinkConnector",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable": false,
        "topics": "rollup",
        "api.key": [API_KEY],
        "transforms":"agentRollup",
        "transforms.agentRollup.type":"com.newrelic.telemetry.AgentRollupTransformation",
        "transforms.agentRollup.event.type":"CONFLUENT_EVENT"
        }
    }
    ```
- This will allow you to post a message like this into the `rollup` topic .    
```

{"agent_rollup":"FASTBPM-27267-OrderProvisioning-ATLAS::zlp34997-orderprovisioning-1-4-atlas-6fb7544d9b-9mh2w","gauge_name":"java.lang:type=MemoryPool,name=Compressed Class Space:Usage.used","capture_time":{TIMESTAMP},"value":1.749964E7,"weight":60}
```
- After you post the message head over to insights and a new event of type `CONFLUENT_EVENT` will be created.
