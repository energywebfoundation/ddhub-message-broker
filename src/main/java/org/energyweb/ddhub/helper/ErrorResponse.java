package org.energyweb.ddhub.helper;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse extends Response {

	public ErrorResponse(String returnCode, String returnMessage) {
		super(returnCode, returnMessage);
	}

}
