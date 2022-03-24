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
	private String did;
	private List<ReturnMessage> success = new ArrayList<ReturnMessage>();
	private List<ReturnMessage> failed = new ArrayList<ReturnMessage>();
}