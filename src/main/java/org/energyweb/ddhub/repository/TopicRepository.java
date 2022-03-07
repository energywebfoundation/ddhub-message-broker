package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTO.SchemaType;
import org.energyweb.ddhub.dto.TopicDTOPage;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.TopicVersion;
import org.jboss.logging.Logger;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {
	@Inject
	TopicVersionRepository topicVersionRepository;

	@Inject
	Logger log;

	public void save(TopicDTO topicDTO) {
		Topic topic = new Topic();
		TopicVersion topicVersion = new TopicVersion();
		try {
			BeanUtils.copyProperties(topic, topicDTO);
			topic.setCreatedBy(topicDTO.getDid());
			;
			topic.setCreatedDate(LocalDateTime.now());
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
			Topic _topic = findById(new ObjectId(topicDTO.getId()));
			topicVersion.setTopicId(_topic.getId());
			Map map = BeanUtils.describe(topicDTO);
			map.remove("id");
			map.remove("name");
			map.remove("owner");
			map.remove("tags");
			topic.setUpdatedBy(topicDTO.getDid());
			topic.setUpdatedDate(LocalDateTime.now());
			BeanUtils.copyProperties(topic, map);
			topic.setId(_topic.getId());
			topic.setName(_topic.getName());
			topic.setOwner(_topic.getOwner());
			topic.setTags(topicDTO.getTags());
			BeanUtils.copyProperties(topicVersion, topic);
			topicVersionRepository.persist(topicVersion);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new MongoException("Unable to update");
		}

		try {
			topic.setUpdatedDate(LocalDateTime.now());
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
		list("ownerdid", ownerDID).forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				map.remove("tags");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()));
				topicDTO.setTags(entity.getTags());
				topicDTO.setSchema(entity.getSchema());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return topicDTOs;
	}

	public void validateTopicIdByOwner(TopicDTO topic, String ownerDID) {
		findByIdOptional(new ObjectId(topic.getId())).filter(data -> data.getOwner().contentEquals(ownerDID))
				.orElseThrow(() -> new MongoException("id:" + topic.getId() + " not exists"));
	}

	public TopicDTOPage queryByOwnerNameTags(String owner, String name, int page, int size, String... tags) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		StringBuffer buffer = new StringBuffer("owner = ?1");
		Optional.ofNullable(name).ifPresent(value -> buffer.append(" and name = ?2"));
		Optional.ofNullable(tags).ifPresent(value -> {
			if (value.length > 0) {
				buffer.append(" and tags in ?3");
			}
		});

		long totalRecord = find(buffer.toString(), owner, name, tags).count();

		PanacheQuery<Topic> topics = find(buffer.toString(), owner, name, tags);
		if (size > 0) {
			topics.page(Page.of((((page - 1) * size) - (page > 1 ? 1 : 0)), size));
		}
		topics.list().forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				map.remove("tags");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()));
				topicDTO.setTags(entity.getTags());
				topicDTO.setSchema(entity.getSchema());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return new TopicDTOPage(totalRecord, size == 0 ? totalRecord : size, page, topicDTOs);
	}

	public List<TopicDTO> countByOwner(String[] owner) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		PanacheQuery<Topic> topics = find("owner in ?1", owner);
		topics.list().forEach(entity -> {
			log.info(entity);
		});
		return topicDTOs;
	}

}
