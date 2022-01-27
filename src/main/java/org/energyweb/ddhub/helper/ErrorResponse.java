package org.energyweb.ddhub.helper;


import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse extends Response {
	@JsonIgnore
	private Integer tmbulkId;

	public ErrorResponse(String returnCode, String returnMessage, int tmbulkId) {
		super(returnCode, returnMessage);
		this.tmbulkId = tmbulkId;
	}

	public ErrorResponse(String returnCode, String returnMessage) {
		super(returnCode, returnMessage);
	}


}
