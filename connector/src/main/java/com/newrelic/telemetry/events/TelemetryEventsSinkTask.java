package com.newrelic.telemetry.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.*;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.*;
import com.newrelic.telemetry.events.models.EventModel;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TelemetryEventsSinkTask extends SinkTask {
  private static Logger log = LoggerFactory.getLogger(TelemetryEventsSinkTask.class);
  String apiKey = null;
  public EventBatchSender eventSender = null;

  EventBuffer eventBuffer = null;

  public EventBatch eventBatch = null;

  ObjectMapper mapper = null;

  int retries = 0;
  long retryInterval = 0l;
  int retriedCount = 0;

  @Override
  public String version() {
    return "1.0.0";
  }


  @Override
  public void start(Map<String, String> map) {

     apiKey = map.get (TelemetrySinkConnectorConfig.API_KEY);
     //accountId = Long.parseLong( map.get(TelemetrySinkConnectorConfig.ACCOUNT_ID));
     retries = map.get(TelemetrySinkConnectorConfig.MAX_RETRIES)!=null? Integer.parseInt( map.get(TelemetrySinkConnectorConfig.MAX_RETRIES)):(Integer) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.MAX_RETRIES);
     retryInterval = map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS)!=null?Long.parseLong( map.get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS)):(Long) TelemetrySinkConnectorConfig.conf().defaultValues().get(TelemetrySinkConnectorConfig.RETRY_INTERVAL_MS);
     mapper = new ObjectMapper();

     try {
         EventBatchSenderFactory eventFactory = EventBatchSenderFactory.fromHttpImplementation((Supplier<HttpPoster>) OkHttpPoster::new);
         eventSender = EventBatchSender.create(eventFactory.configureWith(apiKey).endpointWithPath(new URL("https://insights-collector.newrelic.com/v1/accounts/events")).build());
         eventBuffer = new EventBuffer( new Attributes());
     } catch (MalformedURLException e) {
         e.printStackTrace();
     }



  }

  private Attributes buildAttributes(Map<String, Object> atts){
    Attributes attributes = new Attributes();
    atts.keySet().forEach(key->{
        //if(!(key.equals("timestamp") || key.equals("eventType"))) {
            Object attributeValue = atts.get(key);
            if (attributeValue instanceof String)
                attributes.put(key, (String)attributeValue);
            else if (attributeValue instanceof Number)
                attributes.put(key, (Number)attributeValue);
            else if (attributeValue instanceof Boolean)
                attributes.put(key, (Boolean) attributeValue);
        //}

    });
    return attributes;
  }


  @Override
  public void put(Collection<SinkRecord> records) {


        for(SinkRecord record:records)   {
            try {
                log.info("got back record " + record.toString());
                List<EventModel> dataValues = (List<EventModel>) record.value();
                  dataValues.forEach(eventModel -> {
                                //EventModel eventModel = mapper.convertValue(dataValue, EventModel.class);
                                Event event = new Event(eventModel.eventType, buildAttributes(eventModel.otherFields()), eventModel.timestamp);
                                eventBuffer.addEvent(event);
                            });
            } catch (IllegalArgumentException ie){
                log.error(ie.getMessage());
                //throw ie;
                continue;
            }


        }

    eventBatch  = eventBuffer.createBatch();

    retriedCount = 0;
    while (!circuitBreaker() ){
            if(retriedCount++<retries-1) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    log.error("Sleep thread was interrupted");
                }
            } else
                throw new RuntimeException("failed to connect to new relic after retries");
    }

  }

  private boolean circuitBreaker() {
      try {
          sendToNewRelic();
          return true;
      } catch(RuntimeException e){
            return false;
      }
  }

  private void sendToNewRelic(){
      try{
          Response response = null;
          response = eventSender.sendBatch(eventBatch);
                              log.info("Response from new relic " + response);

          if(!(response.getStatusCode()==200 || response.getStatusCode()==202)) {
              log.error("New Relic sent back error "+response.getStatusMessage());
              throw new RuntimeException(response.getStatusMessage());
          }
      }
      catch(ResponseException re) {
          log.error("New Relic down "+re.getMessage());
          throw new RuntimeException(re);
      }
  }


  @Override
  public void flush(Map<TopicPartition, OffsetAndMetadata> map) {

  }

  @Override
  public void stop() {
    //Close resources here.
  }

}
