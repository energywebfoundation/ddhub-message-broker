package org.energyweb.ddhub.dto;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.energyweb.ddhub.dto.TopicDTO.SchemaType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicDTOCreate {

	@NotNull
	@NotEmpty
	@Pattern(regexp = "^(?:&(#\\d+;|#x[0-9A-Fa-f]+;|lt;|gt;|quot;|amp;)|[^&<>\"'/\\\\\\-\\.\\u0008\\u000C\\u000A\\u000D\\u0009])*$",message = "Invalid characters detected.")
	private String name;
	@NotNull
	private SchemaType schemaType;
	@NotNull
	@NotEmpty
	private String schema;
	@NotNull
	@Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z-.]+)?(\\+[0-9A-Za-z-.]+)?$", message = "Required Semantic Versions")
	private String version;
	@NotNull
	@NotEmpty
	private String owner;
	@Valid
	private Set<@NotEmpty @Pattern(regexp = "^(?:&(#\\d+;|#x[0-9A-Fa-f]+;|lt;|gt;|quot;|amp;)|[^&<>\"'/\\\\\\-\\.\\u0008\\u000C\\u000A\\u000D\\u0009])*$",message = "Invalid characters detected.") String> tags;

	@JsonIgnore
	private String createdBy;
}