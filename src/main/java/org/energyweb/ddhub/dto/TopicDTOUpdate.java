package org.energyweb.ddhub.dto;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({ "schema", "schemaType", "owner", "did", "isOwnerValid", "ownerValid" })
public class TopicDTOUpdate {
	@Valid
	private Set<@NotEmpty @Pattern(regexp = "^[^&<>\"'/\\\\\\-\\.\\r\\n\\t]*$", message = "Invalid characters detected.") String> tags;
}