package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadDTOs extends FileUploadDTO {

	@NotNull
	@FormParam("fqcns")
	private String fqcns;

	@NotNull
	@NotEmpty
	@FormParam("clientGatewayMessageId")
	@Size(max = 200, message = "The maximum length is 200 characters")
	@Pattern(regexp = "^[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-4[0-9a-fA-F]{3}\\-[89abAB][0-9a-fA-F]{3}\\-[0-9a-fA-F]{12}$", message = "Invalid UUID v4 format")
    private String clientGatewayMessageId;

	@NotNull
	@FormParam("payloadEncryption")
	private boolean payloadEncryption;

}
