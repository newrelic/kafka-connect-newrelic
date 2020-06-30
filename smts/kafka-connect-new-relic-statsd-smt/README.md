# Introduction

Welcome to your new Kafka Connect Agent Rollup SMT!

# How to run

- Build the zip using `mvn clean package` and unpack the zip into the kafka connect plugins directory
- This SMT works with the [Metrics Sink connector](https://github.com/newrelic/kafka-connect-newrelic/tree/master/connector#create-a-telemetry-events-connector-job)
# How to run

- Build the zip using `mvn clean package` and unpack the zip into the kafka connect plugins directory
- This SMT works with the Events Sink connector
- Use the following json sample to post a job with this SMT 
    ```
    {
      "name": "statsd-connector",
      "config": {
      "connector.class": "com.newrelic.telemetry.metrics.TelemetryMetricsSinkConnector",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": false,
      "topics": "statsd",
      "account.id": "2738846",
      "api.key": "NRII-APBK3zA-8qdDeiDI-rdm48M5gOyRrdkb",
      "transforms":"statsd",
      "transforms.statsd.type":"com.newrelic.telemetry.StatsdTransformation"
      }
    }
    ```
- This will allow you to post a message like this into the `statsd` topic .    
```
{
  "total_disk": 8578400256,
  "disk_usage": 58.18,
  "eo": "some-agent",
  "cpu_idle": 98.28,
  "av": "0.0.1",
  "cpu_user": 0.98,
  "ts": 1568672499467,
  "free_disk": 3587739648,
  "cpu_steal": 0,
  "group": "system_metrics",
  "env_group": "some_grooup",
  "total_mem": 7891111936,
  "s4": "10.151.11.119",
  "env": "master",
  "cpu_nice": 0.01,
  "tags": {
    "Name": "3c-master",
    "ProcessesName": "druid",
    "Compliance": "None",
    "OwnerEmail": "something@somewhere.com",
    "Project": "druid",
    "Owner": "Rick Deckard",
    "IncidentEmail": "ALERT@somwhere.com",
    "VPC": "services",
    "Newrelic": "true",
    "Department": "Engineering",
    "Organization": "null",
    "ApplicationGroup": "ops",
    "Team": "ops",
    "aws:autoscaling:groupName": "3c-master-argosd-offworld-services-green"
  },
  "mem": 17.28,
  "used_disk": 4990660608,
  "cpu_io_wait": 0.01,
  "an": "over",
  "free_mem": 5086703616,
  "cpu_system": 0.7,
  "cpu": 1.72
}
```
- After you post the message head over to insights and a new `metric`  of type `gauge` will be created.
