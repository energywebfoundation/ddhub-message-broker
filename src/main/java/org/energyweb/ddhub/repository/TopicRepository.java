package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTO.SchemaType;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {
	@Inject
	TopicVersionRepository topicVersionRepository;

	public void save(TopicDTO topicDTO) {
		Topic topic = new Topic();
		TopicVersion topicVersion = new TopicVersion();
		try {
			BeanUtils.copyProperties(topic, topicDTO);
			persist(topic);
			BeanUtils.copyProperties(topicVersion, topic);
			topicVersion.setTopicId(topic.getId());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to save");
		}

		try {
			topicVersionRepository.persist(topicVersion);
			topicDTO.setId(topic.getId().toString());
		} catch (Exception ex) {
			delete(topic);
		}
	}

	public void updateTopic(TopicDTO topicDTO) {
		Topic topic = new Topic();
		TopicVersion topicVersion = new TopicVersion();
		try {
			topic.setId(new ObjectId(topicDTO.getId()));
			topicVersion.setTopicId(new ObjectId(topicDTO.getId()));
			Map map = BeanUtils.describe(topicDTO);
			map.remove("id");
			BeanUtils.copyProperties(topic, map);
			BeanUtils.copyProperties(topicVersion, topic);
			topicVersionRepository.persist(topicVersion);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new MongoException("Unable to update");
		}

		try {
			update(topic);
		} catch (Exception ex) {
			topicVersionRepository.delete(topicVersion);
		}
	}

	public Topic findByName(String name) {
		return find("namespace", name).firstResultOptional()
				.orElseThrow(() -> new MongoException("topic :" + name + " not exists"));
	}

	public List<Topic> findBySchemaType(SchemaType schemaType) {
		return list("schemaType", schemaType);
	}

	public void validateTopicIds(List<String> topicIds) {
		topicIds.forEach(id -> {
			find("_id", new ObjectId(id)).firstResultOptional()
					.orElseThrow(() -> new MongoException("id:" + id + " not exists"));
		});
	}

	public void deleteTopic(String id) {
		try {
			deleteById(new ObjectId(id));
			topicVersionRepository.delete("topicId", new ObjectId(id));
		} catch (Exception e) {
			throw new MongoException("Unable to delete");
		}
	}

}
