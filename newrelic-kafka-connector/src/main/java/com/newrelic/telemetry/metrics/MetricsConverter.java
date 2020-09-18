package com.newrelic.telemetry.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.telemetry.events.models.EventModel;
import com.newrelic.telemetry.metrics.models.CountModel;
import com.newrelic.telemetry.metrics.models.GaugeModel;
import com.newrelic.telemetry.metrics.models.MetricModel;
import com.newrelic.telemetry.metrics.models.SummaryModel;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetricsConverter implements Converter {
    private static Logger log = LoggerFactory.getLogger(MetricsConverter.class);

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] fromConnectData(String s, Schema schema, Object o) {

        List<EventModel> eventModels = (List<EventModel>) o;
        try {
            return new ObjectMapper().writeValueAsString(eventModels).getBytes();
        } catch (JsonProcessingException e) {
            log.error("Error while serializing events");
            return new byte[0];
        }

    }

    @Override
    public SchemaAndValue toConnectData(String s, byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        List<MetricModel> metricModels = new ArrayList<>();

        try {
            List<Map<String, Object>> dataValues = (ArrayList<Map<String, Object>>) mapper.readValue(new String(bytes), ArrayList.class);
            for (Map<String, Object> metricValue : dataValues) {
                if (metricValue.get("metrics") == null) {
                    log.error("Missing metric in message: " + metricValue);
                    continue;
                }
                List<Map<String, Object>> metrics = (List<Map<String, Object>>) metricValue.get("metrics");
                Map<String, Object> commons = (Map<String, Object>) metricValue.get("common");
                Map<String, Object> commonAttributes = commons != null ? (Map<String, Object>) commons.get("attributes") : null;
                if (commons != null)
                    commons.remove("attributes");
                for (Map<String, Object> dataValue : metrics) {
                    //dataValue = (Map<String, Object>) metrics.get("metrics");
                    log.info("this is the attribute" + dataValue.get("attributes"));
                    log.info("this is the type" + dataValue.get("type"));
                    if (commons != null)
                        dataValue.putAll(commons);

                    switch ((String) dataValue.get("type")) {
                        case "gauge":
                            GaugeModel gaugeModel = mapper.convertValue(dataValue, GaugeModel.class);
                            if (commonAttributes != null)
                                gaugeModel.attributes.putAll(commonAttributes);
                            metricModels.add(gaugeModel);
                            break;
                        case "count":
                            CountModel countModel = mapper.convertValue(dataValue, CountModel.class);
                            if (commonAttributes != null)
                                countModel.attributes.putAll(commonAttributes);
                            metricModels.add(countModel);
                            break;
                        case "summary":
                            SummaryModel summaryModel = mapper.convertValue(dataValue, SummaryModel.class);
                            if (commonAttributes != null)
                                summaryModel.attributes.putAll(commonAttributes);
                            metricModels.add(summaryModel);
                            break;
                    }
                }
            }

            return (new SchemaAndValue(null, metricModels));
        }  catch (Exception e) {
            log.error("Error while deserializing events " + e.getMessage());
            throw new SerializationException(e.getMessage());
        }

    }
}
