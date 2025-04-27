package org.energyweb.ddhub.helper;

import java.util.List;
import java.util.Set;

import org.energyweb.ddhub.exception.InvalidPayloadException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.jboss.resteasy.spi.UnhandledException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PayloadValidator {
	
  private static final Set<Character> FORBIDDEN_CHARS = Set.of('&', '<', '>', '"', '\'', '/', '-', '.', '\\','\b', '\f', '\n', '\r', '\t');
  private static final List<String> SUPPORTED_FORMATS = List.of("JSD7", "XSD6", "XML", "CSV", "TSV");

  public static void validate(String format, String schema, String payload) {
    if (!SUPPORTED_FORMATS.contains(format)) {
      throw new IllegalArgumentException("Unsupported format: " + format);
    }
    
    try {
    	switch (format) {
    	case "JSD7":
    		validateJson(payload,schema);
    		break;
    	}
    } catch (Exception e) {
      if (e instanceof IllegalArgumentException || e instanceof JsonParseException || e instanceof ValidationException)
        throw new InvalidPayloadException(e.getMessage());
      throw new UnhandledException("Payload validation failed: " + e.getMessage(), e);
    }
  }
  
  private static void validateJson(String payload, String schema) throws Exception {
	ObjectMapper mapper = new ObjectMapper();
    JsonNode payloadNode = mapper.readTree(payload);
    JsonNode schemaNode = mapper.readTree(schema);
    
    JSONObject schemaJson = new JSONObject(new JSONTokener(schema));
	
    Schema _schema = SchemaLoader.builder()
        .schemaJson(schemaJson)
        .draftV7Support()
        .addFormatValidator(new LocalDateTimeFormatValidator())
        .build()
        .load()
        .build();

    _schema.validate(new JSONObject(payload));

	
    validateJsonNodeRecursively(payloadNode,schemaNode, "");
  }

  private static void validateJsonNodeRecursively(JsonNode node,JsonNode schema, String path) {
    if (node.isObject()) {
    	node.fieldNames().forEachRemaining(field -> {
    		String newPath = path + "/" + field;
    		JsonNode childPayloadNode = node.get(field);
    		JsonNode childSchemaNode = schema.path("properties").path(field);
    		validateJsonNodeRecursively(childPayloadNode, childSchemaNode, newPath);
    	});
    } else if (node.isArray()) {
    	for (int i = 0; i < node.size(); i++) {
    		validateJsonNodeRecursively(node.get(i), schema, path + "[" + i + "]");
    	}
    } else if (node.isTextual()) {
    	boolean isPlainUnconstrainedString = 
    			schema.path("type").asText().equals("string") &&
    		      !schema.has("format") &&
    		      !schema.has("pattern") &&
    		      !schema.has("enum");
        if (isPlainUnconstrainedString) {
        	validateNoSpecialChars(node.asText(), path);
        }
    }
  }

  private static void validateNoSpecialChars(String value, String path) {
    for (char c : value.toCharArray()) {
      if (FORBIDDEN_CHARS.contains(c)) {
        // Will be removing dash (-) and dot (.) based on slack conversation:
        // https://energywebfoundation.slack.com/archives/C030RK4D5TJ/p1745466421296229
        throw new IllegalArgumentException(
            "Payload Invalid characters detected. '" + c + "' at " + path);
      }
    }
  }

}
