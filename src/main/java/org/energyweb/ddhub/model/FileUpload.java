package org.energyweb.ddhub.model;

import java.time.Duration;
import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MongoEntity(collection = "fileupload")
@Getter
@Setter
@NoArgsConstructor
public class FileUpload {
	private ObjectId id;
    private Long maxMsgAge;
    private String fileName;
	private String fqcn;
	private String topicId;
	private String ownerdid;
	private String signature;
	private String clientGatewayMessageId;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	
	
	public boolean validateExpiryContent() {
		return createdDate.plusSeconds(Duration.ofMillis(maxMsgAge).toSeconds()).isBefore(LocalDateTime.now());
	}
}
