package com.newrelic.es.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.newrelic.es.AppCommons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonFlattenFunction implements Function<String, String> {

    private static final Logger log = LoggerFactory.getLogger(JsonFlattenFunction.class);

    private static final ObjectMapper mapper = AppCommons.getObjectMapper();

    private static final String PREFIX_SEPARATOR = ":";

    // 4KB, should be about 2K characters of a String
    private static final int MAX_VALUE_LENGTH = 4000 / 2;

    private final Predicate<String> attributeFilter;

    private final String customEventType;

    public JsonFlattenFunction(String customEventType) {
        this(s -> false, customEventType);
    }

    /**
     * Flatten Json message, removing any attributes matching the
     * {@code attributeFilter}.
     */
    public JsonFlattenFunction(Predicate<String> attributeFilter, String customEventType) {
        this.attributeFilter = attributeFilter;
        this.customEventType = customEventType;
        log.info("Initialize flatten function, customEventType: '{}'", customEventType);
    }

    @Override
    public String apply(String jsonMessage) {
        String flatJson;
        try {
            flatJson = flatten(jsonMessage);
        } catch (IOException e) {
            log.error("Error transforming json message. Error: {}", e.getMessage());
            return null;
        }
        return flatJson;
    }

    private String flatten(String jsonMessage) throws IOException {

        JsonNode rootNode = mapper.readTree(jsonMessage);
        ((ObjectNode) rootNode).put("eventType", customEventType);
        ObjectNode objectNode = mapper.createObjectNode();

        ObjectNode flat = flatten(rootNode, objectNode, "");

        Iterator<Map.Entry<String, JsonNode>> iterator = flat.fields();

        // remove any element values that are too long.
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> currentNode = iterator.next();
            if (currentNode.getValue().size() > MAX_VALUE_LENGTH) {
                log.debug("Truncated node: key {}, value: {}", currentNode.getKey(), currentNode.getValue());
                ((ObjectNode) currentNode).put(currentNode.getKey(), "_NR_TRUNCATED_");
            }
        }

        return mapper.writeValueAsString(flat);
    }

    private ObjectNode flatten(JsonNode jsonNode, ObjectNode objectNode, String keyPrefix) {
        Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> currentNode = iterator.next();

            JsonNode nodeValue = currentNode.getValue();

            // if the node matches the attributeFilter, do not process it.
            if (!attributeFilter.test(currentNode.getKey())) {

                if (nodeValue.getNodeType() == JsonNodeType.STRING || nodeValue.getNodeType() == JsonNodeType.BOOLEAN
                        || nodeValue.getNodeType() == JsonNodeType.NUMBER
                        || nodeValue.getNodeType() == JsonNodeType.NULL) {

                    objectNode.set(keyPrefix + currentNode.getKey(), nodeValue);

                } else if (nodeValue.getNodeType() == JsonNodeType.OBJECT) {
                    flatten(currentNode.getValue(), objectNode, keyPrefix + currentNode.getKey() + PREFIX_SEPARATOR);
                } else if (nodeValue.getNodeType() == JsonNodeType.ARRAY) {
                    Iterator<JsonNode> arrayNode = currentNode.getValue().elements();

                    int arrayIndex = 0;

                    while (arrayNode.hasNext()) {
                        JsonNode next = arrayNode.next();
                        // log.debug("processing array element: {}" + next.toString());

                        if (next.getNodeType() == JsonNodeType.OBJECT) {
                            flatten(next, objectNode,
                                    keyPrefix + currentNode.getKey() + "_" + arrayIndex++ + PREFIX_SEPARATOR);
                        } else {
                            objectNode.set(keyPrefix + currentNode.getKey() + "_" + (arrayIndex++) + PREFIX_SEPARATOR,
                                    next);
                        }

                    }
                } else {
                    log.debug("Unhandled node type: " + nodeValue.getNodeType().name());
                }
            }
        }
        return objectNode;
    }
}
