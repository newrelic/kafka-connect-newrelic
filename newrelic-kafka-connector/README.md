# Welcome to your  Kafka Connect New Relic connector!

## Getting Up and Running.

### Assumptions: 
- Kafka is installed on user’s machine - A quick start can be found on the Kafka
site

-  User is competent with Kafka. Although the basic installation, configuration,
and running of this is straight forward and be done quickly, there are more
advanced topics in maintaining a fault tolerant, enterprise Kafka configuration.
DevOps knowledge of Kafka and their enterprise Kafka stack easily translates
to the concepts of Connect, as Connect is a component of Kafka.

### Installing Kafka Connect for New Relic (Sink) Installing Kafka Connect for New Relic (Sink)

- Download Kafka Connect for New Relic (planning to make binary jars available
on our public GitHub repo - Can someone confirm that is true)
- Download any SMT’s relevant for your environment.
    - In this case we are using test data based on a current customer’s use
case.
    - Custom SMTs (Single Message Transformations) can be developed by
end user or through a NR services engagement. Based on experience
with out home grown solution, this is a highly beneficial capability.
    - New Relic can continue to develop and make available Transforms for
common message formats such as Prometheus
- Create a folder to put your downloaded files into
    ````
      $ mkdir /opt/kafka/plugins
      $ cp ~/Downloads/*.jar /opt/kafka/plugins
    ````
- Configure your plugins directory in Kafka by updating the `connect-distributed.properties` file
    - Update plugin.path to include your plugins directory created above
- Stop and restart Kafka / Connect if it is already running
- To check if your connector is available head over to the connect rest endpoint, by default it will be http://localhost:8083/connector-plugins/. Make sure our 2 connectors are listed `com.newrelic.telemetry.events.TelemetryEventsSinkConnector` and `com.newrelic.telemetry.metrics.TelemetryMetricsSinkConnector`.

### Create a Telemetry Events Connector job

- Use Curl or Postman to post the following json on the Connect rest URL http://localhost:8083/connectors.
  ```
  {
   "name": "events-connector",
   "config": {
   "connector.class": "com.newrelic.telemetry.events.TelemetryEventsSinkConnector",
   "value.converter": "com.newrelic.telemetry.events.EventsConverter",
   "topics": "nrevents",
   "api.key": "<NEW_RELIC_API_KEY>"
   }
  }
  ```
  
  
- This will create the connector job. To check the list of running Connector jobs head over to http://localhost:8083/connectors
- Make sure you see your connector, in this case `events-connector` listed
- To check the status (RUNNING OR PAUSED OR FAILED) use this URL http://localhost:8083/connectors/events-connector/status

### Create a Telemetry METRICS Connector job
- Metrics have the same configuration as events.
- Use Curl or Postman to post the following json on the Connect rest URL http://localhost:8083/connectors.
  ```
  {
   "name": "metrics-connector",
   "config": {
   "connector.class": "com.newrelic.telemetry.events.TelemetryMetricsSinkConnector",
   "value.converter":"com.newrelic.telemetry.metrics.MetricsConverter",
   "value.converter.schemas.enable": false,
   "topics": "nrmetrics",
   "api.key": "<NEW_RELIC_API_KEY>"
   }
  }
    ```
### Full list of variables you can send to connector 
  | attribute     | Required |                          description          |
  | ------------- | -------- | --------------------------------------------- |
  | name          | yes | user definable name for identifying connector |
  |connector.class| yes | com.newrelic.telemetry.events.TelemetryEventsSinkConnector(Events) or com.newrelic.telemetry.events.TelemetryMetricsSinkConnector(Metrics)|
  |value.converter| yes | com.newrelic.telemetry.events.EventsConverter(Events) or com.newrelic.telemetry.metrics.MetricsConverter(Metrics) |
  |topics         | yes | Coma seperated list of topics the connector listens to.|
  |api.key        | yes | NR api key |
  |nr.max.retries | no  | set max number of retries on the NR server, default is 5 |
  |nr.retry.interval.ms | no | set interval between retries in milli seconds, default is 1000 |
  |errors.tolerance | no | all(ignores all json errors) or none(makes connector fail on messages with incorrect format) |
  |errors.deadletterqueue.topic.name| no | dlq topic name ( messages with incorrect format are sent to this topic) |
  |errors.deadletterqueue.topic.replication.factor| no | dlq topic replication factor |
  
  

### Simple Message Transforms 
- Sometimes customers want to use their own message format which is different from the standard `events` or `metrics` format.
- In that case we develop [Simple Message Transforms](https://docs.confluent.io/current/connect/transforms/index.html#:~:text=Kafka%20Connect%20Transformations-,Kafka%20Connect%20Transformations,sent%20to%20a%20sink%20connector.)  
- Currently we have developed two SMTs [Agent Rollup](https://github.com/newrelic/kafka-connect-newrelic/tree/master/smts/Kafka-connect-new-relic-agent-rollup-smt) and [Statsd](https://github.com/newrelic/kafka-connect-newrelic/tree/master/smts/kafka-connect-new-relic-statsd-smt) 
