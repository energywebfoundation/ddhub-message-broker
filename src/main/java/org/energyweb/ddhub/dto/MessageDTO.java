package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageDTO extends DDHub {
   

    @Size(max = 200, message = "The maximum length is 200 characters")
    private String transactionId;

    @NotNull
    @NotEmpty
    @Size(max = 8192, message = "The maximum length is 8192 characters")
    private String payload;
    
    @NotNull
    @NotEmpty
    @Size(max = 1024, message = "The maximum length is 1024 characters")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string")
    private String topicId;
    
    @NotNull
    @NotEmpty
    @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions")
	private String topicVersion;
    
    @NotNull
    @NotEmpty
    private String signature;


    @JsonIgnore
	public String subjectName() {
		if (StringUtils.isBlank(topicId))
			return null;
		return super.streamName().concat(".").concat(topicId);
	}

	public String storageName() {
		return super.streamName() + "/" + subjectName() + "/";
	}
    
}
