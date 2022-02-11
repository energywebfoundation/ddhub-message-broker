package org.energyweb.ddhub.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO.SchemaType;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TopicVersionRepository implements PanacheMongoRepository<TopicVersion> {
    public TopicVersion findByIdAndVersion(String id, String versionNumber) {
        return find("topicId = ?1 and version = ?2", new ObjectId(id), versionNumber).firstResultOptional().orElseThrow(()-> new MongoException("id:" + id + " not exists"));
    }

    public TopicVersion findById(String id) {
        return findByIdOptional(new ObjectId(id)).orElseThrow(()-> new MongoException("id:" + id + " not exists"));
    }

    public List<TopicVersion> findListById(String id) {
        return list("topicId", new ObjectId(id));
    }

    public TopicVersion findByName(String name) {
        return find("namespace", name).firstResultOptional().orElseThrow(()-> new MongoException("topic :" + name + " not exists"));
    }

    public List<TopicVersion> findBySchemaType(SchemaType schemaType) {
        return list("schemaType", schemaType);
    }

}
