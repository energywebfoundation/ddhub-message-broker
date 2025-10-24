package org.energyweb.ddhub.helper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.energyweb.ddhub.exception.InvalidPayloadException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    // === 1. HTML ENCODING CORE ==============================================
    public static String htmlEncode(String input) {
        if (input == null) return null;
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

    public static String htmlDecode(String input) {
        if (input == null) return null;
        String normalized = input
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/")
                .replace("&#x2D;", "-")
                .replace("&#x2E;", ".")
                .replace("&#x5C;", "\\")
                .replace("&#x08;", "\b")
                .replace("&#x0C;", "\f")
                .replace("&#10;", "\n")
                .replace("&#13;", "\r")
                .replace("&#x09;", "\t");

        return StringEscapeUtils.unescapeHtml4(normalized);
    }

    // === 2. TRUSTWAVE ENCODE / DECODE =======================================
    public static String encodeTrustwave(String input) {
        if (input == null) return null;
        try {
            String wrapped = "\"" + input.replace("\"", "\\\"") + "\"";
            input = mapper.readValue(wrapped, String.class);
        } catch (Exception ignored) {
        }
        return htmlEncode(input);
    }

    public static String decodeTrustwave(String input) {
        return htmlDecode(input);
    }

    // === 3. GENERIC ENCODE/DECODE (OBJECT) ==================================
    public static JsonNode encodeValuesOnly(JsonNode node) {
        if (node == null || node.isNull()) return NullNode.instance;

        if (node.isTextual()) {
            return new TextNode(encodeTrustwave(node.asText()));
        }

        if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            for (JsonNode n : node) arr.add(encodeValuesOnly(n));
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
        if (input == null) return null;

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
        if (arr == null) return null;
        return arr.stream()
                  .map(TrustwaveEncoder::encodeTrustwave)
                  .collect(Collectors.toList());
    }
    
    public static Set<String> encodeValuesOnlyArray(Set<String> inputSet) {
        if (inputSet == null) return null;
        return inputSet.stream()
                .map(TrustwaveEncoder::encodeTrustwave)
                .collect(Collectors.toCollection(LinkedHashSet::new)); // preserve insertion order
    }

    public static JsonNode decodeValuesOnly(JsonNode node) {
        if (node == null || node.isNull()) return NullNode.instance;

        if (node.isTextual()) {
            return new TextNode(decodeTrustwave(node.asText()));
        }

        if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            for (JsonNode n : node) arr.add(decodeValuesOnly(n));
            return arr;
        }

        if (node.isObject()) {
            ObjectNode obj = mapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                obj.set(f.getKey(), decodeValuesOnly(f.getValue()));
            }
            return obj;
        }

        return node;
    }

    // === 4. SCHEMA FIELD EXTRACTION ========================================
    public static List<String> listStringTypeOnlyBy(JsonNode schema, String basePtr) {
    	try {
    		 List<String> out = new ArrayList<>();

    	        if (schema == null || schema.isNull()) return out;

    	        JsonNode typeNode = schema.get("type");
    	        if (typeNode != null && typeNode.isTextual() && typeNode.asText().equals("object")) {
    	            JsonNode props = schema.get("properties");
    	            if (props != null && props.isObject()) {
    	                Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
    	                while (fields.hasNext()) {
    	                    Map.Entry<String, JsonNode> f = fields.next();
    	                    String key = f.getKey();
    	                    JsonNode sub = f.getValue();
    	                    String ptr = basePtr + "/" + key;

    	                    String type = sub.path("type").asText();
    	                    boolean noPattern = sub.get("pattern") == null;
    	                    boolean noEnum = sub.get("enum") == null;
    	                    boolean noFormat = sub.get("format") == null;
    	                    boolean noConst = sub.get("const") == null;

    	                    if ("string".equals(type) && noPattern && noEnum && noFormat && noConst) {
    	                        out.add(ptr);
    	                    }
    	                    out.addAll(listStringTypeOnlyBy(sub, ptr));
    	                }
    	            }
    	        }

    	        JsonNode allOf = schema.get("allOf");
    	        if (allOf != null && allOf.isArray()) {
    	            for (JsonNode s : allOf) {
    	                out.addAll(listStringTypeOnlyBy(s, basePtr));
    	            }
    	        }

    	        return new ArrayList<>(new LinkedHashSet<>(out));
    		
    	} catch (Exception e) {
    		throw e;
    	}
       
    }

    // === 5. ENCODE FIELDS BY SCHEMA ========================================
    public static JsonNode encodeFieldsBy(JsonNode data, List<String> ptrs) {
    	try {
    		for (String ptr : ptrs) {
    			String[] parts = ptr.split("/");
    			JsonNode ref = data;
    			JsonNode parent = null;
    			String lastKey = null;
    			
    			for (int i = 1; i < parts.length; i++) { // skip empty first
    				lastKey = parts[i];
    				parent = ref;
    				if (ref == null || ref.isNull()) break;
    				ref = ref.get(lastKey);
    			}
    			
    			if (parent instanceof ObjectNode && lastKey != null) {
    				JsonNode val = parent.get(lastKey);
    				if (val != null && val.isTextual()) {
    					((ObjectNode) parent).put(lastKey, htmlEncode(val.asText()));
    				}
    			}
    		}
    		return data;
    	} catch (Exception e) {
    		throw e;
    	}
    }

    public static String encodeValuesBySchemaString(String payload, String schema) throws InvalidPayloadException {
        try {
        	JsonNode encoded = encodeValuesBySchema(payload, schema);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(encoded);
		} catch (JsonProcessingException e) {
			throw new InvalidPayloadException(e.getMessage());
		}
    }
    
    public static JsonNode encodeValuesBySchema(String payload, String schema) throws InvalidPayloadException {
        // Parse JSON nodes
        JsonNode payloadNode;
        JsonNode schemaNode;
		try {
			payloadNode = mapper.readTree(payload);
			schemaNode = mapper.readTree(schema);
		} catch (Exception e) {
			throw new InvalidPayloadException(e.getMessage());
		}

        // Validate schema using Everit JSON Schema
        JSONObject schemaJson = new JSONObject(new JSONTokener(schema));
        Schema _schema = SchemaLoader.builder()
                .schemaJson(schemaJson)
                .draftV7Support()
                .addFormatValidator(new LocalDateTimeFormatValidator()) // custom validator if needed
                .build()
                .load()
                .build();

        try {
            _schema.validate(new JSONObject(payload)); // throws ValidationException if invalid
        } catch (ValidationException e) {
            System.err.println("Schema validation failed:");
            e.getAllMessages().forEach(System.err::println);
            throw e;
        }

        // Encode valid payload according to schema-defined string fields
        List<String> fields = listStringTypeOnlyBy(schemaNode, "");
        return encodeFieldsBy(payloadNode, fields);
    }

    // // === 7. DEMO ============================================================
    // public static void main(String[] args) throws Exception {
    //     String schemaJson = """
    //     {
    //       "type": "object",
    //       "properties": {
    //         "name": { "type": "string" },
    //         "bio": { "type": "string", "format": "markdown" },
    //         "age": { "type": "number" },
    //         "nested": {
    //           "type": "object",
    //           "properties": {
    //             "desc": { "type": "string" }
    //           }
    //         }
    //       },
    //       "required": ["name"]
    //     }
    //     """;

    //     String payloadJson = """
    //     {
    //       "name": "Alice / Bob",
    //       "bio": "Hello <b>world</b>",
    //       "age": 30,
    //       "nested": { "desc": "Simple text / test" }
    //     }
    //     """;

    //     JsonNode encoded = encodeValuesBySchema(payloadJson, schemaJson);

    //     System.out.println("=== Encoded by Schema ===");
    //     System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(encoded));

    //     JsonNode decoded = decodeValuesOnly(encoded);
    //     System.out.println("\n=== Decoded ===");
    //     System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(decoded));
    // }
}

