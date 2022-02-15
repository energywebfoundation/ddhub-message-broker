package org.energyweb.ddhub.model;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@MongoEntity(collection = "schema_version")
@Data
public class TopicVersion {

	@JsonIgnore
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private ObjectId id;
	private ObjectId topicId;
	private String namespace;
	private String schemaType;
	private String schema;
	private String version;
	
	
}