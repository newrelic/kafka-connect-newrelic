![image](./nr-open-source.png)



# Welcome to the New Relic Kafka Connector Repo!

At present this repository contains only *Sink* type connectors including ones for:

- Events
- Logs
- Metrics

These sink connectors will ship records from Kafka topics to several of New Relic's Ingestion API endpoints, for events, metrics, or logs.
Each New Relic API endpoint (ogs, metrics, events, respectively) uses a separate connector class.

### Installing Kafka Connect for New Relic (Sink) 
There are two options to install:
 - Downloading a published release for GitHub
    1. download the latest [release](https://github.com/newrelic/kafka-connect-newrelic/releases) from this repository
    2. Extract the archive
    3. copy the extracted contents to your Kafka distribution's connect plugins directory (Usually `<kafka-home>/connect-plugins` or `/opt/connectors`)

 - Downloading a published release from Confluent Hub
    1. Download a suitable version from [this page](https://www.confluent.io/hub/newrelic/newrelic-kafka-connector)
    2. copy the extracted contents to your Kafka distribution's connect plugins directory (Usually `<kafka-home>/connect-plugins` or `/opt/connectors`)

 - Building from source:
    1. clone this repository
    2. build with maven:  `mvn package`
    3. copy the contents of `target/components/packages/newrelic-newrelic-kafka-connector-<version>` to your Kafka distribution's connect plugins directory (Usually `<kafka-home>/connect-plugins` or `/opt/connectors`)

### Using the Connectors

You should configure your connector with one of the following classes depending on the type of telemetry you are sending:
- `com.newrelic.telemetry.events.EventsSinkConnector`
- `com.newrelic.telemetry.logs.LogsSinkConnector`
- `com.newrelic.telemetry.metrics.MetricsSinkConnector`

All of the connectors expect either structured data with a schema (usually provided by the Avro, Protobuf, or JSON w/ Schema convertors), or a Java Map (usually provided by the schemaless JSON converter).

#### Timestamps
Records sent to New Relic should contain a field named `timestamp`, else the current timestamp will be assigned when the record is flushed to the API.  Timestamps are usually [Unix epoch timestamps](https://docs.newrelic.com/docs/logs/ui-data/timestamp-support/#unix).
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
  |nr.region      | yes  | NR data region, either US or EU (default: US) |
  |nr.client.timeout | no | Time, in milliseconds, to wait for a response from the New Relic API (default is 2000)|
  |nr.client.proxy.host| no | Proxy host to use to connect to the New Relic API |
  |nr.client.proxt.port | no | Proxy host to use to connect to the New Relic API (required if using a proxy host) | 
  |nr.flush.max.records | no  | The maximum number of records to send in a payload. (default: 1000) |
  |nr.flush.max.interval.ms | no  | Maximum amount of time in milliseconds to wait before flushing records to the New Relic API. (default: 5000) |
  

### Sample Configuration
This is basic sample configuration for an Event connector in .properties format:
```
name=newrelic-logs-sink-connector

connector.class=com.newrelic.telemetry.logs.LogsSinkConnector

# configure this based on your workload
tasks.max=1

topics=newrelic-logs-sink-topic
api.key=<api.keyi>
nr.region=US

# messages are stored in schemaless json on the topic
# you could use Avro, Protobuf, etc here as well
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false

# declare the transformations
transforms=inserttimestamp

#Insert the timestamp from the Kafka record -- comment this transform if you wish.  New Relic will assign a timestamp on ingest.
transforms.inserttimestamp.type=org.apache.kafka.connect.transforms.InsertField$Value
transforms.inserttimestamp.timestamp.field=timestamp
```

See other properties files examples [here](./newrelic-kafka-connector/config)


## Contributing
Full details about how to contribute to
Contributions to improve Kafka Connect for New Relic are encouraged! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.
To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at opensource@newrelic.com.

## License
Kafka Connect for New Relic is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.
>If applicable: The Kafka Connect for New Relic also uses source code from third party libraries. Full details on which libraries are used and the terms under which they are licensed can be found in the third party notices document.
