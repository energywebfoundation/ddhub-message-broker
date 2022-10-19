package org.energyweb.ddhub.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchMessageDTO {

	@JsonIgnore
	private String fqcn;

	@NotNull
	@Valid
	private List<@NotNull @NotEmpty @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") String> topicId;

	@NotNull
	@Valid
	private List<@NotNull @NotNull String> senderId;

	@Pattern(regexp = "^[a-zA-Z0-9\\-:.>*]+$", message = "Required Alphanumeric string")
	private String clientId = "mb-default";

	private int amount = 1;
	
	private boolean ack = false;

	@JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
	private LocalDateTime from;

	@JsonIgnore
	public String subjectAll() {
		return streamName().concat(".").concat("*");
	}

	@JsonIgnore
	public String subjectName(String topicId) {
		return streamName().concat(".").concat(topicId);
	}

	@JsonIgnore
	public String streamName() {
		String[] streamName = fqcn.split(java.util.regex.Pattern.quote("."));
		Collections.reverse(Arrays.asList(streamName));
		return String.join("_", streamName);
	}

	public String findDurable() {
		List<String> _clientId = new ArrayList<>();
		_clientId.addAll(Arrays.asList(clientId.split("[.>*]")));
		_clientId.removeIf(String::isEmpty);
		return String.join(":", _clientId);
	}

	public int fetchAmount(long totalAckPending) {
        int fetchAmount = amount;
        if(totalAckPending > 0 && totalAckPending > amount) {
            fetchAmount = (int) totalAckPending;
        }
        return (fetchAmount > MessageAckDTOs.MAX_FETCH_AMOUNT)?MessageAckDTOs.MAX_FETCH_AMOUNT:fetchAmount;
    }
}
