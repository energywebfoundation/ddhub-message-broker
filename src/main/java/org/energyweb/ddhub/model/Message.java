package org.energyweb.ddhub.model;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "message")
@Data
@NoArgsConstructor
public class Message {
	private ObjectId id;
	private String transactionId;
    private String topicId;
	private String topicVersion;
	private String fqcn;
	private String senderdid;
    private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
}
