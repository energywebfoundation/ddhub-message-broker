package org.energyweb.ddhub.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

import javax.enterprise.context.ApplicationScoped;

import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.Topic.SchemaType;

import java.util.List;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {
    public Topic findByName(String name) {
        return find("namespace", name).firstResult();
    }

    public List<Topic> findBySchemaType(SchemaType schemaType) {
        return list("schemaType", schemaType);
    }

}
