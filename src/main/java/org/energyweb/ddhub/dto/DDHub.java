package org.energyweb.ddhub.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.energyweb.ddhub.helper.ErrorResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DDHub {

	@JsonIgnore
	private String fqcn;

	@JsonIgnore
	public String streamName() {
		String[] streamName = fqcn.split(Pattern.quote("."));
		Collections.reverse(Arrays.asList(streamName));
		return String.join("_", streamName);
	}
	
	@JsonIgnore
	public ErrorResponse validateAnonymousKey(String validateKey) {
		final String regex = "^((did|DID|dID|dId|DiD|DId|Did):).+(\\w*)";
		ErrorResponse errorMessage = null;
		if(Pattern.compile(regex).matcher(validateKey).matches()) {
			errorMessage = new ErrorResponse("18", "validateKey:" + validateKey + " DID format identify");
		}
		return errorMessage;
	}
}