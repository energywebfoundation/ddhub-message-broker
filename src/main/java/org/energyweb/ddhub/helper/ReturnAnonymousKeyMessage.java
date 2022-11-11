package org.energyweb.ddhub.helper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ReturnAnonymousKeyMessage {
	
	@NonNull
	private String key;
	@NonNull
	private String status;
	@NonNull
	private String message;
}
