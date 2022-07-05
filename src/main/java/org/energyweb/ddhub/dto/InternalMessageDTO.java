package org.energyweb.ddhub.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InternalMessageDTO {
   
	@NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
	private String fqcn;
    
    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String clientGatewayMessageId;

    @NotNull
    @NotEmpty
    private String payload;
    
    @JsonIgnore
    private String id;
    
    @JsonIgnore
    private String senderDid;

    @JsonIgnore
    private String geatewayDid;
    
    @JsonIgnore
	private long timestampNanos;
    
	@JsonIgnore
	public String streamName() {
		String[] streamName = fqcn.split(Pattern.quote("."));
		Collections.reverse(Arrays.asList(streamName));
		return String.join("_", streamName);
	}
    
    
    
}
