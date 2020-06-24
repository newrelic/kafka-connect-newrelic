
# Transform Examples

There are two example transforms outline below which represent real world customer usage in `kafka2nrapi` which we'd like to recreate using Connect.

The of the two examples is more complex as it requires parsing the value of one of the payloads attributes. The coded transformation classes for both examples are included in the [code-examples](../code-examples) directory.

## Example 1

The customer wanted the `agent_rollup` value to be parsed out and assigned to new attributes before being sent to the Events API. `agent_rollup`'s value was "packed" in a way that could be broken into smaller components.


### Original Message - Example 1

[link](./event-example-01-input.json) to event-example-01.input.json
```json
{
  "agent_rollup": "FASTBPM-27267-OrderProvisioning-ATLAS::zlp34997-orderprovisioning-1-4-atlas-6fb7544d9b-9mh2w",
  "gauge_name": "java.lang:type=MemoryPool,name=Compressed Class Space:Usage.used",
  "capture_time": 1588169400000,
  "value": 1.749964E7,
  "weight": 60
}
```


### Transformed Event Message - Example 1

[link](./event-example-01-transformed.json) to event-example-01.transformed.json
```json
{
  "appName": "FASTBPM",
  "appID": "27267",
  "service": "OrderProvisioning-ATLAS",
  "nodeName": "zlp34997",
  "svcOrPodName": "orderprovisioning-1-4-atlas-6fb7544d9b-9mh2w",
  "agent_rollup": "FASTBPM-27267-OrderProvisioning-ATLAS::zlp34997-orderprovisioning-1-4-atlas-6fb7544d9b-9mh2w",
  "gauge_name": "java.lang:type=MemoryPool,name=Compressed Class Space:Usage.used",
  "timestamp": 1588169400000,
  "capture_time": 1588169400000,
  "value": 1.749964E7,
  "weight": 60,
  "eventType": "CUSTOM_EVENT"
}
```

### Transform Logic - Example 1

Transform performed by [JsonTransformFunction.java](../code-examples/JsonTransformFunction.java)

#### `agent_rollup`'s value format:

*Note*: `NODE_NAME` and `SVC_OR_POD_NAME` may appear in any order following the `::`, however the `NODE_NAME` always matches the following regex: `[a-zA-Z]{3,4}[0-9]{3,5}`. The remaining is considered to be the `SVC_OR_POD_NAME`.


```
APP_NAME-APP_ID-SERVICE::[NODE_NAME-SVC_OR_POD_NAME]
```
_or_
```
APP_NAME-APP_ID-SERVICE::[SVC_OR_POD_NAME-NODE_NAME]
```

where:

| part            | required | notes                                              |
|:----------------|:---------|:---------------------------------------------------|
| APP_NAME        | yes      | text up to first dash (`-`)                        |
| APP_ID          | yes      | text between first and secong dash (`-`)           |
| SERVICE         | yes      | *all* remaining text after second dash (`-`)       |
| NODE_NAME       | no       | text after `::` matching `[a-zA-Z]{3,4}[0-9]{3,5}` |
| SVC_OR_POD_NAME | no       | all text after first dash (`-`), following `::`    |

#### Mapping

| part            | mapped attribute |
|:----------------|:-----------------|
| APP_NAME        | `appName`        |
| APP_ID          | `appId           |
| SERVICE         | `service`        |
| NODE_NAME       | `nodeName`       |
| SVC_OR_POD_NAME | `svcOrPodName`   |

#### Valid values for `agent_rollup`:

##### Minimal Message:

* `APP-12345-Service::`
* transformed: `{"appName":"APP","appID":"12345","service":"Service"}`

##### Minimal Message where the `service` field contains a dash (`-`)

* `APP-12345-Service-SomethingElse::`
* transformed: `{"appName":"APP","appID":"12345","service":"Service-SomethingElse"}`

##### All fields: appName, appId, Service, nodeName (first), & svcOrPodName (second)

* `APP-12345-Service::abc12345-svcOrNode`
* transformed:

```json
{
  "appName":"APP",
  "appID":"12345",
  "service":"Service",
  "nodeName":"abc12345",
  "svcOrPodName":"svcOrNode"
}
```

##### All fields: appName, appId, Service, svcOrPodName (first), & nodeName (second)

* `APP-12345-Service::abc12345-svcOrNode`
* transformed:

```json
{
  "appName":"APP",
  "appID":"12345",
  "service":"Service",
  "nodeName":"abc12345",
  "svcOrPodName":"svcOrNode"
}
```

##### All fields plus additional data after svcOrNodeName

* `APP-12345-Service::abc12345-svcOrNode-1-4-atlas-6fb7544d9b-9mh2w`
* transformed:

```json
{
  "appName":"APP",
  "appID":"12345",
  "service":"Service",
  "nodeName":"abc12345",
  "svcOrPodName":"svcOrNode-1-4-atlas-6fb7544d9b-9mh2w"}
```

## Example 2

Customer had the following message types in a Kafka topic and wanted to be able to ingest them into New Relic.

Transform performed by

### Original Message - Example 2

[link](./event-example-02-input.json) to event-example-02.input.json

```json
{
  "ts": 1568672499467,
  "total_disk": 8578400256,
  "free_disk": 3587739648,
  "used_disk": 4990660608,
  "cpu_idle": 98.28,
  "cpu_user": 0.98,
  "cpu_steal": 0,
  "cpu_nice": 0.01,
  "cpu_io_wait": 0.01,
  "cpu_system": 0.7,
  "cpu": 1.72,
  "total_mem": 7891111936,
  "free_mem": 5086703616,
  "mem": 17.28,
  "env": "pre-prod",
  "tags": {
    "TeamEmail": "preprodops@newrelic.com",
    "Team": "DataOps",
    "aws:autoscaling:groupName": "nr-autoscale-1234-dataops-preprod"
  }
}
```

### Transformed Event Message - Example 2

[link](./event-example-02-transformed.json) to event-example-02.transformed.json
```json
{
  "timestamp": 1568672499467,
  "eventType": "ec2ServerMetrics",
  "total_disk": 8578400256,
  "free_disk": 3587739648,
  "used_disk": 4990660608,
  "cpu_idle": 98.28,
  "cpu_user": 0.98,
  "cpu_steal": 0,
  "cpu_nice": 0.01,
  "cpu_io_wait": 0.01,
  "cpu_system": 0.7,
  "cpu": 1.72,
  "total_mem": 7891111936,
  "free_mem": 5086703616,
  "mem": 17.28,
  "env": "pre-prod",
  "tags:Team": "DataOps",
  "tags:aws:autoscaling:groupName": "nr-autoscale-1234-dataops-preprod"
}
```

### Transform Logic - Example 2

Transform performed by [JsonFlattenFunction.java](../code-examples/JsonFlattenFunction.java)

Several things need to be performed in this transformation:

* Flatten the JSON structure. NR Events does not accept events with embedded objects or arrays. It looks like Kafka Connect has a similar feature out of the box.
* Map inbound `ts` attribute name to `timestamp`. Currently hardcoded, but should be configurable via a Java `Function` or similar.
* Filter out any attributes that may contain personal information or other irrelevant information. This is accomplished by passing a Java `Predicate` to the constructor during instantiation. Any matching attributes are discarded.
* Inject `eventType` attribute and value defined in the configuration.
