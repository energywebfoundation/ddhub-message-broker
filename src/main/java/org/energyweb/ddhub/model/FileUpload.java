package org.energyweb.ddhub.model;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "fileupload")
@Data
@NoArgsConstructor
public class FileUpload {
	private ObjectId id;
    private Long maxMsgAge;
    private String fileName;
	private String fqcn;
	private String topicId;
	private String ownerdid;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
}
