package org.energyweb.ddhub.model;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;

@MongoEntity(collection = "schema")
@Data
public class Topic {
	private ObjectId id;
	private String namespace;
	private String schemaType;
	private String schema;
	private String version;
	private String owner;
}