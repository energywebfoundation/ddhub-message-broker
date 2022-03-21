package org.energyweb.ddhub.model;

import java.time.LocalDateTime;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@MongoEntity(collection = "schema_version")
@Getter
@Setter
public class TopicVersion {

	@JsonIgnore
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private ObjectId id;
	private ObjectId topicId;
	private String name;
	private String schemaType;
	private String schema;
	private String version;
	private String owner;
	private Set<String> tags;
	private String createdBy;
	private String updatedBy;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
}