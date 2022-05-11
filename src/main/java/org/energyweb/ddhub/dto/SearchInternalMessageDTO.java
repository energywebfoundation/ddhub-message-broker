package org.energyweb.ddhub.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
public class SearchInternalMessageDTO  {
	@JsonIgnore
	private String fqcn;

	@Valid
	private List<@NotNull @NotEmpty String> senderId;

	@Pattern(regexp = "^[a-zA-Z0-9\\-:]+$", message = "Required Alphanumeric string")
	private String clientId = "mb-default-internal";

	private int amount = 1;

	@JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
	private LocalDateTime from;

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

	public String findDurable(String clientIdPostfix) {
		if(Optional.ofNullable(from).isPresent()) {
			return clientId.concat(Long.toString(from.toEpochSecond(ZoneOffset.UTC))).concat(streamName());
		}
		return clientId.concat(clientIdPostfix).concat(streamName());
	}
}
