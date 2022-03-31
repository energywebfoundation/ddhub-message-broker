package org.energyweb.ddhub.helper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ReturnMessage{
	private String did;
	private String messageId;
	private Integer statusCode;
	private ReturnErrorMessage err = null;
	
}