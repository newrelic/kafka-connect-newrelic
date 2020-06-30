Kafka Connect for New Relic

The following items are included in this doc:

* [requirements](requirements.md) document
* [Event messages](messages/event-messages.md) document with several examples and transformation logic
* [message transform classes](code-examples) we use today
* [kafka2nrapi](kafka2nrapi.pdf) PDF overview

# Getting Up and Running

### Assumptions:
* Kafka is installed on user’s machine - A quickstart can be found on the Kafka site
* User is competent with Kafka. Although the basic installation, configuration, and running of this is straight forward and be done quickly, there are more advanced topics in maintaining a fault tolerant, enterprise Kafka configuration. DevOps knowledge of Kafka and their enterprise Kafka stack easily translates to the concepts of Connect, as Connect is a component of Kafka.


### Installing Kafka Connect for New Relic (Sink)
* Download Kafka Connect for New Relic (planning to make binary jars available on our public GitHub repo - Can someone confirm that is true)
* Download any SMT’s relevant for your environment.
    * In this case we are using test data based on a current customer’s use case.
    * Custom SMTs (Single Message Transformations) can be developed by end user or through a NR services engagement. Based on experience with out home grown solution, this is a highly beneficial capability.
    * New Relic can continue to develop and make available Transforms for common message formats such as Prometheus
* Create a folder to put your downloaded files into
```bash
$ mkdir /opt/kafka/plugins
$ cp ~/Downloads/*.jar /opt/kafka/plugins
```
* Configure your plugins directory in Kafka by updating the `connect-distributed.properties` file
  * Update `plugin.path` to include your plugins directory created above
* _Stop and restart Kafka / Connect if it is already running_
* Refer the [Events](https://github.com/newrelic/kafka-connect-newrelic/tree/master/connector#create-a-telemetry-events-connector-job) , [Metric](https://github.com/newrelic/kafka-connect-newrelic/tree/master/connector#create-a-telemetry-metrics-connector-job) , [Agent Rollup transformer](https://github.com/newrelic/kafka-connect-newrelic/tree/master/smts/Kafka-connect-new-relic-agent-rollup-smt) or [Statsd](https://github.com/newrelic/kafka-connect-newrelic/tree/master/smts/kafka-connect-new-relic-statsd-smt) transformer for further details 



### Build/Packaging
- All the artifacts are using maven packaging
- To package the artifact we are using the standard [kafka-connect-maven-plugin](https://docs.confluent.io/5.5.0/connect/managing/confluent-hub/kafka-connect-maven-plugin/kafka-connect-mojo.html#componentTypes)
- This package will be generated as a zip which you need to unpack into the Connect plugins folder.
- The zip will be emailed to confluent-hub@confluent.io, for getting it reviewed and added into [Confluent hub](https://www.confluent.io/hub/)
- Refer the [Connector POM](https://github.com/newrelic/kafka-connect-newrelic/blob/master/connector/pom.xml#L107) to see the plugin details 
- The plugin will [generate the zip with a manifest](https://docs.confluent.io/current/connect/managing/confluent-hub/component-archive.html#confluent-hub-component-archive) which will be used by reviewers to add it to Confluent hub, as well as it will be used to generate things like Tags, Logos, description etc.
- The Connector POM only has a single sink connector hence the [componentype](https://github.com/newrelic/kafka-connect-newrelic/blob/master/connector/pom.xml#L140) is `sink`. The [Statsd POM](https://github.com/newrelic/kafka-connect-newrelic/blob/master/smts/kafka-connect-new-relic-statsd-smt/pom.xml#L129) has componentype `transform`


### Connect/framework details that NewRelic customers should know

#### Error Handling
- Connect framework support several message formats like bitearray, string, JSON, AVRO, protobuf etc
- We  decided the connector to support JSON
- Customers can still decide to use Avro/Protobuff etc
    
    * There are several benefits if the customer decides to do so 
    
        - The customer can setup their own error handling strategies as described by this [blog](https://www.confluent.io/blog/kafka-connect-deep-dive-error-handling-dead-letter-queues/)
        - As mentioned in this blog messages which dont comply with valid message formats can be redirected to DLQs which make bad message tracking easily traceable 

- If customer decides to go with a plain JSON topic, we have code level validation which looks for required fields and their formats. But this will not be able to use DLQs. Bad messages will be logged and the Connector will move on to next message. This will make bad message tracking harder but Customers can use Splunk/Kibana alerts to get around this.
- We are using batching wherever possible, which means all the Kafka messages found in a single poll are batched up in one single call to New Relic. This reduces the number of hits to New Relic but increases the payload size. Customer understands that this can result in message loss if the payload size exceeds [max payload size](https://docs.newrelic.com/docs/data-ingest-apis/get-data-new-relic/metric-api/metric-api-limits-restricted-attributes). To make sure that does not happen pass in these parameters which creating connector in the config json [consumer.max.poll.interval.ms](https://docs.confluent.io/current/installation/configuration/consumer-configs.html#max.poll.interval.ms) and [consumer.max.poll.records](https://docs.confluent.io/current/installation/configuration/consumer-configs.html#max.poll.records). *Note that you have to add prefix `consumer.` to the above consumer properties.*

#### Security
- Kafka connect job configurations can be viewed at the rest interface for example http://localhost:8083/connectors/[connector-name]/config
- This will result in exposing your API-KEY. To avoid this customer has a few options 

  * Customers should think about white listing the Connect url only to certain IPs. In a K8s environment it can be easily done by restricting connect service to a Clusterip.
  * In environments where this is not possible, they will have to use [config providers](https://docs.confluent.io/current/connect/security.html#externalizing-secrets). Kafka provides 2 config providers [File](https://docs.confluent.io/current/connect/security.html#fileconfigprovider) and [InternalSecret (Topic based) ](https://docs.confluent.io/current/connect/security.html#internalsecretconfigprovider). Customers can also look at the API to build their own Config providers.

- Also remember if Kafka rest interface is publicly available it can result in attacks. Customers should whitelist IPs allowed to hit the rest interface. Another approach is to run connect in [standalone mode](https://docs.confluent.io/3.2.0/connect/userguide.html#standalone-mode) where the Connect workers are started with hard coded cofiguration in properties files. 


