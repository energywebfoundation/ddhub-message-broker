package org.energyweb.ddhub.helper;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ReturnStatusMessage {
	
	@NonNull
	private String name;
	@NonNull
	private List<ReturnMessage> details = new ArrayList<ReturnMessage>();
	
	
	public ReturnStatusMessage addDetails(List<ReturnMessage> data) {
		if(data.size() > 0) {
			details.addAll(data);
		}
		return this;
	} 
}
