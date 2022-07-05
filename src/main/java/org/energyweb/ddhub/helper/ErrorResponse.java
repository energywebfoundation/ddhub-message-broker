package org.energyweb.ddhub.helper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse extends DDHubResponse {

	public ErrorResponse(String returnCode, String returnMessage) {
		super(returnCode, returnMessage);
	}

}
