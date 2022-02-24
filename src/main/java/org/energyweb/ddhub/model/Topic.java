package org.energyweb.ddhub.model;

import java.time.LocalDateTime;
import java.util.Set;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;

@MongoEntity(collection = "schema")
@Data
public class Topic {
	private ObjectId id;
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