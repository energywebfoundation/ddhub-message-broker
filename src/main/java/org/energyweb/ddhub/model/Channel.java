package org.energyweb.ddhub.model;

import java.util.List;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "channel")
@Data
@NoArgsConstructor
public class Channel {
	private ObjectId id;
    private Long maxMsgAge;
    private Long maxMsgSize;
    private List<String> topicIds;
    private List<String> admins;
    private List<String> pubsub;
    private Boolean encryption;
	private String fqcn;

}
