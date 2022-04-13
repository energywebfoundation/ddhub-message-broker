package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@JsonIgnoreProperties({"name","schemaType","owner","did","isOwnerValid","ownerValid"})
public class TopicDTOSchema{
	@NotNull
	@NotEmpty
	private String schema;
}