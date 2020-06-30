package com.newrelic.telemetry.functional;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
public class AgentRollupTransformationFunctionalTest {

    // Load the properties required for the producer
    private static Properties producerConnectionProps = new Properties();
    static {
        producerConnectionProps.put("bootstrap.servers", "localhost:9092");
        producerConnectionProps.put("acks", "all");
        producerConnectionProps.put("delivery.timeout.ms", 30000);
        producerConnectionProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConnectionProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    }
    @Test
    public  void testRollup() {
        Producer<String, String> kafkaProducer = new KafkaProducer<>(producerConnectionProps);
        String file = "/Users/rahul/nr/kafka-connect-new-relic-telemetry/smts/Kafka-connect-new-relic-agent-rollup-smt/src/test/java/com/newrelic/telemetry/functional/gauges.json";
        String topic1 = "confluent";
        int lineCount = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                // skip empty lines
                if (!line.trim().equals("")) {
                    String timestamp = Long.toString(System.currentTimeMillis());
                    String lineToPublish = line.replace("{TIMESTAMP}", timestamp);
                    kafkaProducer.send(new ProducerRecord<>(topic1, lineToPublish)).get();
                    lineCount++;
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

}
