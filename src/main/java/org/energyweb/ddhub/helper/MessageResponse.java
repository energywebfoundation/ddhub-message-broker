package org.energyweb.ddhub.helper;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class MessageResponse {
	private String clientGatewayMessageId;
	private Recipients recipients;
	private List<ReturnStatusMessage> status = new ArrayList<ReturnStatusMessage>();
	public void add(List<ReturnMessage> success, List<ReturnMessage> failed) {
		if(success.size() > 0) {
			this.getStatus().add(new ReturnStatusMessage("SENT").addDetails(success));
		}
		if(failed.size() > 0) {
			this.getStatus().add(new ReturnStatusMessage("FAILED").addDetails(failed));
		}
	}
}