package org.energyweb.ddhub.helper;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseWithStatus extends DDHubResponse {
	private List<ReturnAnonymousKeyMessage> status;

	public ResponseWithStatus(String returnCode, String returnMessage, List<ReturnAnonymousKeyMessage> status) {
		super(returnCode, returnMessage);
		this.status = status;
	}

}
