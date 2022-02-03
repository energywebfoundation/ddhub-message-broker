package org.energyweb.ddhub.model;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;

@MongoEntity(collection = "schema")
@Data
public class Topic{
	
	public enum SchemaType {
		JSD7,
	    XSD,
	    STRING
	}

	@JsonIgnore
	private ObjectId id;
    private String namespace;
    private SchemaType schemaType;
    private String schema;


}