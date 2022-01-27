package org.energyweb.ddhub.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Response {
	private String requestId;
	private String refId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd HH:mm:ss")
	private Date timestamp = new Date();
	private final String returnCode;
	private final String returnMessage;
}