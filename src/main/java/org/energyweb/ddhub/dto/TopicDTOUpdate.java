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
	private Set<@NotEmpty @Pattern(regexp = "^(?:&(#\\d+;|#x[0-9A-Fa-f]+;|lt;|gt;|quot;|amp;)|[^&<>\"'/\\\\\\-\\.\\u0008\\u000C\\u000A\\u000D\\u0009])*$",message = "Invalid characters detected.") String> tags;
}