package org.energyweb.ddhub.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MongoEntity(collection = "role_owner")
@Getter
@Setter
@NoArgsConstructor
public class RoleOwner {
	private ObjectId id;
    private Set<String> claimType = new HashSet<String>();
    private String did;
    private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
}
