package org.energyweb.ddhub.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.bson.types.ObjectId;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.Topic.SchemaType;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {
	@Inject
	TopicVersionRepository topicVersionRepository;
	
	public void save(Topic topic) {
		persist(topic);
		try {
			BasicDBObject compositeKey = new BasicDBObject();
			compositeKey.append("id", topic.getId());
			compositeKey.append("version", 1);
			
			TopicVersion topicVersion = new TopicVersion();
			topicVersion.setTopicId(topic.getId());
			topicVersion.setNamespace(topic.getNamespace());
			topicVersion.setSchema(topic.getSchema());
			topicVersion.setSchemaType(topic.getSchemaType());
			topicVersion.setVersion(1);
			topicVersionRepository.persist(topicVersion);
		}catch(Exception ex) {
			delete(topic);
		}
	}
	
	public void updateTopic(Topic topic) {
		BasicDBObject compositeKey = new BasicDBObject();
		compositeKey.append("id", topic.getId());
		compositeKey.append("version", topic.getVersion() + 1);
		
		TopicVersion topicVersion = new TopicVersion();
		topicVersion.setTopicId(topic.getId());
		topicVersion.setNamespace(topic.getNamespace());
		topicVersion.setSchema(topic.getSchema());
		topicVersion.setSchemaType(topic.getSchemaType());
		topicVersion.setVersion(topic.getVersion() + 1);
		topicVersionRepository.persist(topicVersion);
		
		topic.setVersion(topicVersion.getVersion());
		update(topic);
	}
	
    public Topic findByName(String name) {
        return find("namespace", name).firstResult();
    }

    public List<Topic> findBySchemaType(SchemaType schemaType) {
        return list("schemaType", schemaType);
    }
    
    public void validateTopicIds(List<String> topicIds) {
		topicIds.forEach(id -> {
			Topic channelDTO = findById(new ObjectId(id));
			if(channelDTO == null) {
				throw new MongoException("id:" + id + " not exists");
			}
		});
	}

}
