package org.energyweb.ddhub.helper;

import java.util.HashMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ReturnErrorMessage {
	@NonNull
	public  String code;
	@NonNull
	public String reason;
	public HashMap additionalInformation;

}