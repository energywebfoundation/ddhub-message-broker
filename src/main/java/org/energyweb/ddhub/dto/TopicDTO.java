package org.energyweb.ddhub.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;

@MongoEntity(collection = "schema")
@Data
public class TopicDTO {

	public enum SchemaType {
		JSD7,
		XSD,
		STRING
	}

	private String id;
	@NotNull
	@NotEmpty
	private String namespace;
	@NotNull
	private SchemaType schemaType;
	@NotNull
	@NotEmpty
	private String schema;
	@NotNull
	@Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions")
	private String version;

}