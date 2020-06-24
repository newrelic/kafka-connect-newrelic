package com.newrelic.es.transform.att;

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
import java.util.regex.Pattern;

public class JsonTransformFunction implements Function<String, String> {

    private static final Logger log = LoggerFactory.getLogger(JsonTransformFunction.class);

    private static final ObjectMapper mapper = AppCommons.getObjectMapper();

    private static final String AGENT_ROLLUP_KEY = "agent_rollup";
    private static final String CAPTURE_TIME_KEY = "capture_time";


    private static final String APP_NAME_KEY = "appName";
    private static final String APP_ID_KEY = "appID";
    private static final String SERVICE_KEY = "service";
    private static final String NODE_NAME_KEY = "nodeName";
    private static final String SVC_OR_POD_NAME_KEY = "svcOrPodName";
    private static final String TIMESTAMP_KEY = "timestamp";

    // matches the pattern for a node name found in the agent_rollup field
    private static final Pattern NODE_PATTERN = Pattern.compile("[a-zA-Z]{3,4}[0-9]{3,5}");


    // 4KB, should be about 2K characters of a String
    private static final int MAX_VALUE_LENGTH = 4000 / 2;

    private final Predicate<String> attributeFilter;

    private final String customEventType;

    public JsonTransformFunction(String customEventType) {
        this(s -> false, customEventType);
    }

    /**
     * Transform Json message, removing any attributes matching the
     * {@code attributeFilter}.
     */
    public JsonTransformFunction(Predicate<String> attributeFilter, String customEventType) {
        this.attributeFilter = attributeFilter;
        this.customEventType = customEventType;
        log.info("Initialize flatten function, customEventType: '{}'", customEventType);
    }

    @Override
    public String apply(String jsonMessage) {
        String flatJson;
        try {
            flatJson = transform(jsonMessage);
        } catch (IOException e) {
            log.error("Error transforming json message. Error: {}", e.getMessage());
            return null;
        }
        return flatJson;
    }

    private String transform(String jsonMessage) throws IOException {

        JsonNode rootNode = mapper.readTree(jsonMessage);
        ((ObjectNode) rootNode).put("eventType", customEventType);
        ObjectNode objectNode = mapper.createObjectNode();

        ObjectNode transformedMsg = transform(rootNode, objectNode);

        return mapper.writeValueAsString(transformedMsg);
    }

    private ObjectNode transform(JsonNode jsonNode, ObjectNode objectNode) {
        Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> currentNode = iterator.next();

            JsonNode nodeValue = currentNode.getValue();

            // if the node matches the attributeFilter, do not process it.
            if (!attributeFilter.test(currentNode.getKey())) {

                if (nodeValue.getNodeType() == JsonNodeType.STRING || nodeValue.getNodeType() == JsonNodeType.BOOLEAN
                        || nodeValue.getNodeType() == JsonNodeType.NUMBER
                        || nodeValue.getNodeType() == JsonNodeType.NULL) {

                    if (currentNode.getKey().equals(AGENT_ROLLUP_KEY)) {

                        String[] values = nodeValue.toString().replace("\"", "").split("::");

                        // Parse appName, appId, and service
                        if (values.length > 0) {
                            String[] pieces = values[0].split("-", 3);

                            if (pieces.length > 1) {
                                String appName = pieces[0];
                                String appId = pieces[1];
                                String service = null;

                                // all remainging peices become service name
                                if (pieces.length > 2) {
                                    service = pieces[2];
                                }

                                objectNode.put(APP_NAME_KEY, appName);
                                objectNode.put(APP_ID_KEY, appId);
                                objectNode.put(SERVICE_KEY, service);
                            }
                        }

                        // Parse nodeName, svcOrPodName, and nodeExtra
                        if (values.length > 1) {
                            String[] pieces = values[1].split("-");

                            String nodeName = null;
                            String svcPodName = null;

                            if (pieces.length > 1) {
                                // node in first field
                                if (pieces[0].matches(NODE_PATTERN.pattern())) {
                                    nodeName = pieces[0];
                                    svcPodName = concatFields(pieces, 1, pieces.length);
                                } else { // node in last field
                                    nodeName = pieces[pieces.length -1];
                                    svcPodName = concatFields(pieces, 0, pieces.length - 1);
                                }
                            }
                            objectNode.put(NODE_NAME_KEY, nodeName);
                            objectNode.put(SVC_OR_POD_NAME_KEY, svcPodName);
                        }
                    } else if (currentNode.getKey().equals(CAPTURE_TIME_KEY)) {
                        objectNode.put(TIMESTAMP_KEY, nodeValue.asLong());
                    }

                    objectNode.set(currentNode.getKey(), nodeValue);

                } else if (nodeValue.getNodeType() == JsonNodeType.OBJECT ||
                           nodeValue.getNodeType() == JsonNodeType.ARRAY) {
                    log.info("Current transform function does not support object values");
                } else {
                    log.debug("Unhandled node type: " + nodeValue.getNodeType().name());
                }
            }
        }
        return objectNode;
    }

    /**
     * Concatenates the elements in an array by placing a dash (-) between the elements.
     * Also provides the ability to not include elements from the beginning or end of the array.
     *
     * @param elements array containing string to join
     * @param start element representing the first of the final string
     * @param end element representing the last of the final string
     */
    private String concatFields(String[] elements, int start, int end) {
        StringBuilder sb = new StringBuilder();

        for (int i = start; i < end; i++) {
            sb.append(elements[i]);
            if (i < end - 1) {
                sb.append("-");
            }
        }
        return sb.toString();
    }
}
