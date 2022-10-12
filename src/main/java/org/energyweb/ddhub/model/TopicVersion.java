package org.energyweb.ddhub.model;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.Setter;

@MongoEntity(collection = "schema_version")
@Getter
@Setter
public class TopicVersion {
	private ObjectId id;
	private ObjectId topicId;
	private String schema;
	private String version;
	private String createdBy;
	private String updatedBy;
	private boolean deleted;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	private LocalDateTime deletedDate;
}