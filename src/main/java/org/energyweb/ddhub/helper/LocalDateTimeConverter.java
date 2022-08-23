package org.energyweb.ddhub.helper;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.ws.rs.ext.ParamConverter;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class LocalDateTimeConverter implements ParamConverter<LocalDateTime> {
	@Override
	public LocalDateTime fromString(String value) {

		DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
		org.joda.time.LocalDateTime parsedDate = dtf.parseLocalDateTime(value);
		return LocalDateTime.ofInstant(parsedDate.toDate().toInstant(), ZoneId.systemDefault());
	}
	
	@Override
	public String toString(LocalDateTime value) {
		return value.toString();
	}
}