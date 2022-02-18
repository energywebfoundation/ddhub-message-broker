package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public void save(TopicDTO topic2) {
		Topic topic = new Topic();
		TopicVersion topicVersion = new TopicVersion();
		try {
			Map map = BeanUtils.describe(topic2);
			map.remove("id");
			BeanUtils.copyProperties(topic, map);
			persist(topic);
			BeanUtils.copyProperties(topicVersion, topic);
			topicVersion.setTopicId(topic.getId());
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new MongoException("Unable to save");
		}

		try {
			topicVersionRepository.persist(topicVersion);
			topic2.setId(topic.getId().toString());
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

	public void validateTopicIds(List<String> topicIds) {
		topicIds.forEach(id -> {
			findByIdOptional(new ObjectId(id)).orElseThrow(() -> new MongoException("id:" + id + " not exists"));
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

	public List<TopicDTO> listAllBy(String ownerDID) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
    	list("owner",ownerDID).forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()));
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
    	return topicDTOs;
	}

	public void validateTopicIdByOwner(TopicDTO topic, String ownerDID) {
		findByIdOptional(new ObjectId(topic.getId())).filter(data->data.getOwner().contentEquals(ownerDID)).orElseThrow(() -> new MongoException("id:" + topic.getId() + " not exists"));
	}



}
