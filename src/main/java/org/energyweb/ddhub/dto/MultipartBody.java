package org.energyweb.ddhub.dto;

import java.io.InputStream;

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
public class MultipartBody extends DDHub {
    @NotNull
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream file;

    @NotNull
    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    public String fileName;

    @NotNull
    @FormParam("signature")
    @PartType(MediaType.TEXT_PLAIN)
    public String signature;
    
    @Size(max = 200, message = "The maximum length is 200 characters")
    @NotNull
	@FormParam("topicId")
	@PartType(MediaType.TEXT_PLAIN)
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string")
	private String topicId;
    
    @JsonIgnore
   	public String getSubjectName() {
   		if (StringUtils.isBlank(topicId))
   			return null;
   		return getStreamName().concat(".").concat(topicId);
   	}
}
