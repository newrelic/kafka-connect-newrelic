# Welcome to the New Relic Kafka Connect Sink Connector!

This Kafka Connect Sink will ship records from Kafka topics to several of New Relic's Ingestion API endpoints, for events, metrics, or logs.
Each endpoint uses a separate connector class, requiring a separate running instance or work cluster for each.
See below for examples of how to use them.

### Installing Kafka Connect for New Relic (Sink) 
There are two options to install:
- Downloading a published release
    1. download the latest release from this repository
    2. Extract the archive
    3. copy the extracted contents to your Kafka distribution's connect plugins directory.  (Usually `<kafka-home>/connect-plugins`)
 - Building from source:
     1. clone this repository
     2. build with maven:  `mvn package`
     3. copy the contents of `target/components/packages/newrelic-newrelic-kafka-connector-<version>` to your Kafka distribution's connect plugins directory.  (Usually `<kafka-home>/connect-plugins`)

### Using the Connectors

You should configure your connector with one of the following classes depending on the type of telemetry you are sending:
- `com.newrelic.telemetry.events.EventsSinkConnector`
- `com.newrelic.telemetry.logs.LogsSinkConnector`
- `com.newrelic.telemetry.metrics.MetricsSinkConnector`

All of the connectors expect either structured data with a schema (usually provided by the Avro, Protobuf, or JSON w/ Schema convertors), or a Java Map (usually provided by the schemaless JSON converter).

#### Timestamps
Records sent to New Relic should contain a field named `timestamp`, else the current timestamp will be assigned when the record is flushed to the API.
Consider using the [Replace Field](https://docs.confluent.io/platform/current/connect/transforms/replacefield.html) transformation to rename a field in the payload that is named something other than `timestamp`

Example:
```
"transforms": "RenameField",
"transforms.RenameField.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
"transforms.RenameField.renames": "timeOfTheEvent:timestamp"
```
or use the `timestamp.field` feature of the [InsertField](https://docs.confluent.io/platform/current/connect/transforms/insertfield.html) transform to use the underlying Kafka Record's timestamp as the New Relic timestamp.

Example:
```
"transforms": "InsertTimestamp",
"transforms.InsertTimestamp.type": "org.apache.kafka.connect.transforms.InsertField$Value",
"transforms.InsertTimestamp.timestamp.field": "timestamp"
```

#### Telemetry-specific considerations

##### Events
Events sent to new relic must contain a field named `eventType`.  Consider using the InsertField or ReplaceField transforms to add it to the payload.
Additionally, Events can only contain key/value pairs one level deep. The [Flatten](https://docs.confluent.io/platform/current/connect/transforms/flatten.html) transform can handle this for you. 
See the New Relic Events [JSON formatting guidelines](https://docs.newrelic.com/docs/telemetry-data-platform/ingest-apis/introduction-event-api/#instrument) for specifics.

#### Logs
Logs can assume any structured format, but a field name `message` will be parsed automatically by the platform.
See the New Relic Logs [payload format documentation](https://docs.newrelic.com/docs/logs/log-management/log-api/introduction-log-api/#payload-format) for specifics.

#### Metrics.
Metrics must contain a `name`, `type`, and `value`.  Some metric types require additional fields.
See the New Relic Metrics [API doucmentation](https://docs.newrelic.com/docs/telemetry-data-platform/ingest-apis/report-metrics-metric-api/#new-relic-guidelines) for specifics.

### Full list of connector configuration options (in addition to global options for all connectors)
  | attribute     | Required |                          description          |
  | ------------- | -------- | --------------------------------------------- |
  |name          | yes | user definable name for identifying connector |
  |connector.class| yes | com.newrelic.telemetry.events.EventsSinkConnector(Events), com.newrelic.telemetry.metrics.MetricsSinkConnector(Metrics), or com.newrelic.telemetry.logs.LogsSinkConnector(Logs)|
  |topics         | yes | Comma seperated list of topics the connector listens to.|
  |api.key        | yes | NR api key |
  |nr.client.timeout | no | Time, in milliseconds, to wait for a response from the New Relic API (default is 2000)|
  |nr.client.proxy.host| no | Proxy host to use to connect to the New Relic API |
  |nr.client.proxt.port | no | Proxy host to use to connect to the New Relic API (required if using a proxy host) | 
  |nr.flush.max.records | no  | The maximum number of records to send in a payload. (default: 1000) |
  |nr.flush.max.interval.ms | no  | Maximum amount of time in milliseconds to wait before flushing records to the New Relic API. (default: 5000) |
  

### Sample Configuration
This is a sample configuration for an Event connector in .properties format:
```
name=newrelic-events-sink-connector

# switch to com.newrelic.telemetry.logs.LogsSinkConnector or com.newrelic.telemetry.metrics.MetricsSinkConnector
connector.class=com.newrelic.telemetry.events.EventsSinkConnector

# configure this based on your workload
tasks.max=1

topics=my-topic
api.key=<api-key>

# messages are stored in schemaless json on the topic
# you could use Avro, Protobuf, etc here as well
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false

# declare the transformations
transforms=inserttimestamp,eventtype,flatten

#Insert the timestamp from the Kafka record
transforms.inserttimestamp.type=org.apache.kafka.connect.transforms.InsertField$Value
transforms.inserttimestamp.timestamp.field=timestamp

# we know all events on this topic represent a purchase, so set 'eventType' to 'purchaseEvent'
transforms.eventtype.type=org.apache.kafka.connect.transforms.InsertField$Value
transforms.eventtype.static.field=eventType
transforms.eventtype.static.value=purchaseEvent

# flatten all nested json fields, using . as a delimeter
transforms.flatten.type=org.apache.kafka.connect.transforms.Flatten\$Value
transforms.flatten.delimiter=.
```
