# Welcome to your  Kafka Connect New Relic connector!

## Getting Up and Running.

### Assumptions: 
- Kafka is installed on user’s machine - A quickstart can be found on the Kafka
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
- Create a plugin configuration file based on the template below TODO: let's
  add a template of this to the repo under the docs folder or similar for developers to refer to.
  ```
  {
   "name": "smt-connector",
   "config": {
   "connector.class": "com.newrelic.telemetry.TelemetrySinkConnector",
   "value.converter": "org.apache.kafka.connect.json.JsonConverter",
   "value.converter.schemas.enable": false,
   "topics": "nrevents",
   "account.id": "<NEW_RELIC_ACCOUNT_ID>",
   "api.key": "<NEW_RELIC_API_KEY>",
   "data.type":"event",
   "transforms":"agentRollup",
  
  "transforms.agentRollup.type":"com.newrelic.telemetry.AgentRollupTransf
  ormation",
   "transforms.agentRollup.event.type":"SMT_EVENT"
   }
  }
    ```
  where...
  
  | attribute     |                          description          |
  | ------------- | --------------------------------------------- |
  | name          | user definable name for identifying connector |
  |connector.class| Fully qualified class name                    |
  |value.converter| JSON convertor                                |
  |value.converter.schemas.enable| false for no schema check      |
  |topics         | Coma seperated list of topics the connector listens to.|
  |account.id     | NR account id |
  |api.key        | NR api key |
  |data.type      | Event or Metric |
  |transforms     | Name your transform for instance `agentRollup` |
  |transforms.[name from above].type | Fully qualified class of the transform |
  |transforms.[name from above].[transorm field] | Config fields defined in the transform |
  
- start kafka if it is not already running
- start kafka connect
  `$ ./bin/connect-distributed.sh config/connect-distributed.properties`
- In a web browser, navigate to [connector URL](http://localhost:8083) to view the list of loaded connectors 