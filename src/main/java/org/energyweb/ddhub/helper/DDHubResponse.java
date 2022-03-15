package org.energyweb.ddhub.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DDHubResponse {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd HH:mm:ss")
	private Date timestamp = new Date();
	@NonNull
	private String returnCode;
	@NonNull
	private String returnMessage;
}