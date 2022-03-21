package org.energyweb.ddhub.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class DDHubResponse {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd HH:mm:ss")
	@Setter(value = AccessLevel.NONE)
	private Date timestamp = new Date();
	@NonNull
	private String returnCode;
	@NonNull
	@Setter(value = AccessLevel.NONE)
	private String returnMessage;
}