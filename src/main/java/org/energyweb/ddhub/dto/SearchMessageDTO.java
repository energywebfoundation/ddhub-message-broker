package org.energyweb.ddhub.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
    
    @NotNull
    @NotEmpty
    private String clientId;

    @NotNull
    @Min(value = 1)
    private int amount;
    
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX") 
    private LocalDateTime from;
    
    @JsonIgnore
	public String subjectAll() {
		return streamName().concat(".").concat("*");
	}


	@JsonIgnore
	public String streamName() {
		String[] streamName = fqcn.split(java.util.regex.Pattern.quote("."));
		Collections.reverse(Arrays.asList(streamName));
		return String.join("_", streamName);
	}
}
