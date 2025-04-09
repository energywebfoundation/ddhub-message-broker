package org.energyweb.ddhub.dto;

import java.io.InputStream;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadDTO extends DDHub {
    @NotNull
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private InputStream file;

    @NotNull
    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    @Pattern(regexp = "^.*\\.(csv|tsv)$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Accepted file type .csv and .tsv ")
    private String fileName;
    
    @FormParam("transactionId")
    @PartType(MediaType.TEXT_PLAIN)
    private String transactionId;

    @NotNull
    @FormParam("signature")
    @PartType(MediaType.TEXT_PLAIN)
    @Pattern(regexp = "^[^&<>\"'/\\-.]*$", message = "Contains unsafe characters & < > \" ' / - . are not allowed")
    private String signature;
    
    @Size(max = 200, message = "The maximum length is 200 characters")
    @NotNull
	@FormParam("topicId")
	@PartType(MediaType.TEXT_PLAIN)
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string")
	private String topicId;
    
    @NotNull
    @NotEmpty
    @FormParam("topicVersion")
	@PartType(MediaType.TEXT_PLAIN)
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z-.]+)?(\\+[0-9A-Za-z-.]+)?$", message = "Required Semantic Versions")
	private String topicVersion;
    
    private String ownerdid;
    
}
