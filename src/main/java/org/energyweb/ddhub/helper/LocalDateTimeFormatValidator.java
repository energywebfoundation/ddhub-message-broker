package org.energyweb.ddhub.helper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.everit.json.schema.FormatValidator;

public class LocalDateTimeFormatValidator implements FormatValidator {

  @Override
  public Optional<String> validate(String subject) {
	  try {
		  LocalDateTime.parse(subject);
		  return Optional.empty();
	  } catch (DateTimeParseException ignored) {}
	  
	  try {
		  OffsetDateTime.parse(subject);
		  return Optional.empty();
	  } catch (DateTimeParseException ignored) {}
	  return Optional.of("Invalid local-date-time format: " + subject);
  }

  @Override
  public String formatName() {
    return "local-date-time";
  }
}

