package org.energyweb.ddhub.helper;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * TrustwaveEncoder
 */
public class TrustwaveEncoder {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TrustwaveEncoder() {
    }

    // === 1. HTML ENCODING CORE ==============================================
    public static String htmlEncode(String input) {
        if (input == null)
            return null;
        String encoded = StringEscapeUtils.escapeHtml4(input);

        encoded = encoded
                .replace("&apos;", "&#x27;") // Trustwave: use &#x27;
                .replace("/", "&#x2F;")
                .replace("-", "&#x2D;")
                .replace(".", "&#x2E;")
                .replace("\\", "&#x5C;")
                .replace("\b", "&#x08;")
                .replace("\f", "&#x0C;")
                .replace("\n", "&#10;")
                .replace("\r", "&#13;")
                .replace("\t", "&#x09;");
        return encoded;
    }

    // === 2. TRUSTWAVE ENCODE ================================================
    public static String encodeTrustwave(String input) {
        if (input == null)
            return null;
        try {
            String wrapped = "\"" + input.replace("\"", "\\\"") + "\"";
            input = mapper.readValue(wrapped, String.class);
        } catch (Exception ignored) {
            // Not a valid JSON string literal: encode the raw input as-is
        }
        return htmlEncode(input);
    }

    // === 3. GENERIC ENCODE (OBJECT) =========================================
    public static JsonNode encodeValuesOnly(JsonNode node) {
        if (node == null || node.isNull())
            return NullNode.instance;

        if (node.isTextual()) {
            return new TextNode(encodeTrustwave(node.asText()));
        }

        if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            for (JsonNode n : node)
                arr.add(encodeValuesOnly(n));
            return arr;
        }

        if (node.isObject()) {
            ObjectNode obj = mapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                obj.set(f.getKey(), encodeValuesOnly(f.getValue()));
            }
            return obj;
        }

        return node;
    }

    public static String encodeValuesOnly(String input) {
        if (input == null)
            return null;

        try {
            // Try to parse as JSON (object, array, or primitive)
            JsonNode root = mapper.readTree(input);
            JsonNode encoded = encodeValuesOnly(root);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(encoded);
        } catch (Exception e) {
            // If invalid JSON, treat as plain string
            return encodeTrustwave(input);
        }
    }

    public static List<String> encodeValuesOnlyArray(List<String> arr) {
        if (arr == null)
            return Collections.emptyList();
        return arr.stream()
                .map(TrustwaveEncoder::encodeTrustwave)
                .collect(Collectors.toList());
    }

    public static Set<String> encodeValuesOnlyArray(Set<String> inputSet) {
        if (inputSet == null)
            return new LinkedHashSet<>();
        return inputSet.stream()
                .map(TrustwaveEncoder::encodeTrustwave)
                .collect(Collectors.toCollection(LinkedHashSet::new)); // preserve insertion order
    }
}
