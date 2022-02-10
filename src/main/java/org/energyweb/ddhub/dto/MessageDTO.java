package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageDTO extends DDHub {
   

    @Size(max = 200, message = "The maximum length is 200 characters")
    private String correlationId;

    @NotNull
    @NotEmpty
    @Size(max = 2048, message = "The maximum length is 2048 characters")
    private String payload;
    
    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string")
    private String topicId;

    @JsonIgnore
	public String getSubjectName() {
		if (StringUtils.isBlank(topicId))
			return null;
		return getStreamName().concat(".").concat(topicId);
	}
    
}
