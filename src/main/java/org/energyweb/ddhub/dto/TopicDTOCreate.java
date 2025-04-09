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
	@Pattern(regexp = "^[^&<>\"'/\\-.]*$", message = "Contains unsafe characters & < > \" ' / - . are not allowed")
	private String name;
	@NotNull
	private SchemaType schemaType;
	@NotNull
	@NotEmpty
	private String schema;
	@NotNull
	@Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions")
	private String version;
	@NotNull
	@NotEmpty
	private String owner;
	@Valid
	private Set<@NotEmpty @Pattern(regexp = "^[^&<>\"'/\\-.]*$", message = "Contains unsafe characters & < > \" ' / - . are not allowed") String> tags;

	@JsonIgnore
	private String createdBy;
}