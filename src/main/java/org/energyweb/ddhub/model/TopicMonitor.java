package org.energyweb.ddhub.model;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@MongoEntity(collection = "topic_monitor")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class TopicMonitor {
	private ObjectId id;
	@NonNull
	private String owner;
    private Long lastTopicUpdate = TimeUnit.NANOSECONDS.toNanos(new Date().getTime());
	private Long lastTopicVersionUpdate = TimeUnit.NANOSECONDS.toNanos(new Date().getTime());
}
