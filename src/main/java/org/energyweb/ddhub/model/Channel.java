package org.energyweb.ddhub.model;

import java.time.LocalDateTime;
import java.util.Set;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MongoEntity(collection = "channel")
@Getter
@Setter
@NoArgsConstructor
public class Channel {
	private ObjectId id;
    private Long maxMsgAge;
    private Long maxMsgSize;
    private Set<String> topicIds;
    private Set<String> admins;
    private Set<String> pubsub;
    private Boolean encryption;
	private String fqcn;
    private String ownerdid;
    private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
}
