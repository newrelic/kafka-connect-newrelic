# Guidance for Kafka Connect New Relic

## Background

### Purpose of this Document

Document project goals, high-level requirements, and technical details to be used during implementation engagement.

###  Engagement Goal

Provide New Relic customers the ability to send messages from a Kafka topic(s) to one of New Relic's Core Telemetry API for ingestion into the New Relic Platform.

These Core Telemetry APIs include the following:

* [Event API]
* [Metric API]
* [Log API]
* [Trace API]

### Motivation

New Relic has several customers who publish performance and telemetry data to Kafka. This practice is done independently of New Relic. Therefore the message formats are do not always align with the New Relic API endpoint messages. There is often a need to perform transformation and filtering on the customer's messages prior to sending to New Relic.

###  Current Solution

New Relic engaged in a PoC with a customer looking to send their metric, event, and logging data to New Relic. Their internal applications and infrastructure were publishing different message types onto a series of topics.

At the time of the PoC the developers were not aware of Kafka Connect, so this effort was coded without the benefit of know about the connect solution. That is a decision wasn't made to "not use Connect because of x, y, z...". We were simply unaware of it.

The New Relic service team engages with the customer and wrote a freestanding Java application named `kafka2nrapi` to achieve the following:

* consume messages from 1 or more topics in the customer's environment
* based on which topic a message from consumed from, transform the message
* based on the message type, send it to appropriate NR API endpoint

The application was highly configurable through a its [application_properties.yml](kafka2nrapi/application_properties.yml) config file.

kafka2nrapi works from a concept of flows, where a flow is identified by the kafka topic being consumed from. This allows the end user to define a transformation class & NR API endpoint for that particular topic.

The application flow of `kafka2nrapi` is as follows (based on configuration file):

#### Consume messages from topic
Kafka client in application subscribes to topic. For each message received, the message is put into an object that tracks details about that message as it flows through the application. These include ingest time, offset, topic, partition, etc.

#### Transform message
For each message consumed, send message payload to configured transform function. Once the message has been transformed, it is placed into a queue where multiple transformed messages are queued for concatenation, compression, and submission to NR API.

#### Aggregate & compress messages
Multiple messages are concatenated together and zipped until one of two thresholds are exceeded. These are: 1. configurable amount of time has elapsed _or_ 2. the compressed messages size (bytes) exceeds a threshold. In our case it is 1MB, as the events API has a limit on the max message size. The zipped messages is a json array of NR event objects that will be the payload of the POST to the New Relic API. This payload is stored in a wrapper class that contains an array of all of the offset contained in this payload. If there is failure during delivery, we are able to log out the failed offsets, however we do not do any retry logic or report this back to kafka.

#### POST message to NR API
The payload is retrieve from the wrapper class (described in the previous section) and an attempt is made to POST the message to the NR API endpoint. If there is a failure during deliver the list of offsets included in the POST's payload is logged out.

#### Known Limitations
* No retry logic when posting the NR
* No logic to reattach disconnected kafka clients
* No support for logistical kafka client items, such as re-balancing, etc
* Transformation logic is currently hard coded into compiled Java classes. If user wants something outside of a basic transformation, they will need to write a custom transformer. Although loading of transformer classes can be configured external to the delivered jar, this is still not desirable.
* The application can scale horizontally, however that needs to managed by the customer by starting multiple instances and defining which partition they will consume from.

# Kafka Connector New Relic

`kafka-connector-newrelic` is meant to replace `kafka2nrapi`, while adding fault tolerance, scalability, and standardization by utilizing [Kafka Connect]

### High-level Goals

#### General

1. Create a Kafka Connector for New Relic which customers can utilize to send telemetry related data stored in their Kafka to New Relic. Initially, targeting NR Events, and then including Metrics, Logs, and Trace data.
2. The end user should be able to define or configure transformation logic for transforming their message formats (json for this iteration) to a New Relic format.
3. Transformation logic errors or data errors should fail as soon as possible and the user should be able to investigate the nature of the failure (see Error Handling section below).
4. Solution should be fault tolerant. This should be achievable through configuration that can be performed by the end user.
5. Solution should be scalable. This should be achievable through configuration that can be performed by the end user.
6. Metrics should be collected / exposed about Connector. For example: throughput (messages processed), successful count, error count (by error class if available)

#### Error Handling

It seems there will several classifications of errors that can arise:
* Data transform errors - the messages consumed from Kafka do not conform to or match the user defined transformation directives / configuration. There may be a case where all messages can not be transformed or just specific messages.
* Data compliance errors - New Relic has certain limitations on API messages such as required attributes, reserved attributes that are not allowed, attribute naming requirements, data types and structures, attribute name and value max length requirements, message length requirements, max attributes, etc.
* Deserialization errors - due to malformed or corrupt messages that may have mis-matched quotation marks or missing or mismatched opening / closing object symbols (`{` and `}`)
* Data content errors - where a validly structured payload is sent to NR and NR returns a `2xx` response, however certain events in the payload are rejected due to being valid messages.
* Errors communicating with New Relic API due to network outage and similar.

#### Outstanding / TODO

* Define a data model (entities) that are able to validate transformed messages post transformation to ensure each message (or message piece in the case of array of messages) complies with the NR data model and can be ingested after a successful post. This may be part of the NR Java Telemetry API already or we may have to create validators in Connect code. Fail early!
* Define / understand / document how we're going to allow the user to configure a message transformation from their Kafka message type to a valid NR message type. Is this KQL?
* Define and document how the failure conditions described above will be handled in the code. We can create an external document mapping "Failure Type (classification)" to "Handling Behavior".
  * Author unit tests that exercise these conditions.






[Event API]: https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api
[Metric API]: https://docs.newrelic.com/docs/data-ingest-apis/get-data-new-relic/metric-api/introduction-metric-api
[Log API]: https://docs.newrelic.com/docs/logs/new-relic-logs/log-api/introduction-log-api
[Trace API]: https://docs.newrelic.com/docs/understand-dependencies/distributed-tracing/trace-api/introduction-trace-api
[Kafka Connect]: https://kafka.apache.org/documentation/#connect
