package org.energyweb.ddhub.helper;

import java.util.List;
import java.util.Set;

import org.energyweb.ddhub.exception.InvalidPayloadException;
import org.jboss.resteasy.spi.UnhandledException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PayloadValidator {

  // Will be removing dash (-) and dot (.) based on slack conversation:
  // https://energywebfoundation.slack.com/archives/C030RK4D5TJ/p1745466421296229
  private static final Set<Character> FORBIDDEN_CHARS = Set.of('&', '<', '>', '"', '\'', '/', '\\', '\n', '\r');
  private static final List<String> SUPPORTED_FORMATS = List.of("JSD7", "XSD6", "XML", "CSV", "TSV");

  public static void validate(String format, String payload) {
    if (!SUPPORTED_FORMATS.contains(format)) {
      throw new IllegalArgumentException("Unsupported format: " + format);
    }

    try {
      switch (format) {
        case "JSD7":
        case "XSD6":
          validateJson(payload);
          break;
      }
    } catch (Exception e) {
      if (e instanceof IllegalArgumentException)
        throw new InvalidPayloadException(e.getMessage());
      throw new UnhandledException("Payload validation failed: " + e.getMessage(), e);
    }
  }

  private static void validateJson(String raw) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(raw);
    validateJsonNodeRecursively(root, "");
  }

  private static void validateJsonNodeRecursively(JsonNode node, String path) {
    if (node.isObject()) {
      node.fields().forEachRemaining(e -> validateJsonNodeRecursively(e.getValue(), path + "/" + e.getKey()));
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        validateJsonNodeRecursively(node.get(i), path + "[" + i + "]");
      }
    } else if (node.isTextual()) {
      validateNoSpecialChars(node.asText(), path);
    }
  }

  private static void validateNoSpecialChars(String value, String path) {
    for (char c : value.toCharArray()) {
      if (FORBIDDEN_CHARS.contains(c)) {
        // Will be removing dash (-) and dot (.) based on slack conversation:
        // https://energywebfoundation.slack.com/archives/C030RK4D5TJ/p1745466421296229
        throw new IllegalArgumentException(
            "Payload contains unsafe character '" + c + "' at " + path + " — characters & < > \" ' / are not allowed");
      }
    }
  }

}
