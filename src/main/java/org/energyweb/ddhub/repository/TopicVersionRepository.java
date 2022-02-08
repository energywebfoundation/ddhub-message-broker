package org.energyweb.ddhub.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.bson.types.ObjectId;
import org.energyweb.ddhub.model.Topic.SchemaType;
import org.energyweb.ddhub.model.TopicVersion;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TopicVersionRepository implements PanacheMongoRepository<TopicVersion> {
    public TopicVersion findByIdAndVersion(String id, Integer versionNumber) {
    	return find("topicId = ?1 and version = ?2", new ObjectId(id),versionNumber).firstResult();
    }

    public TopicVersion findById(String id) {
        return find("_id", id).firstResult();
    }

    public List<TopicVersion> findListById(String id) {
        return list("topicId", new ObjectId(id));
    }

    public TopicVersion findByName(String name) {
        return find("namespace", name).firstResult();
    }

    public List<TopicVersion> findBySchemaType(SchemaType schemaType) {
        return list("schemaType", schemaType);
    }

}
