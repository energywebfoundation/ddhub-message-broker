package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTO.SchemaType;
import org.energyweb.ddhub.dto.TopicDTOPage;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.TopicVersion;
import org.jboss.logging.Logger;

import com.mongodb.MongoException;

import io.quarkus.cache.CacheKey;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {
	@Inject
	TopicVersionRepository topicVersionRepository;

	@Inject
	Logger logger;

//	@CacheInvalidateAll(cacheName = "topic")
//	@CacheInvalidateAll(cacheName = "tversion")
	public void save(TopicDTO topicDTO) {
		Topic topic = new Topic();
		TopicVersion topicVersion = new TopicVersion();
		try {
			BeanUtils.copyProperties(topic, topicDTO);
			topic.setCreatedBy(topicDTO.did());
			topic.setCreatedDate(LocalDateTime.now());
			persist(topic);
			BeanUtils.copyProperties(topicVersion, topic);
			topicVersion.setVersion(topicDTO.getVersion());
			topicVersion.setSchema(topicDTO.schemaValue());
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
		topicDTO.setDid(null);
	}

//	@CacheInvalidateAll(cacheName = "topic")
//	@CacheInvalidateAll(cacheName = "tversion")
	public void updateTopic(TopicDTO topicDTO) {
		Topic topic = new Topic();
		try {
			Topic _topic = findById(new ObjectId(topicDTO.getId()));
			Map map = BeanUtils.describe(topicDTO);
			map.remove("id");
			map.remove("name");
			map.remove("owner");
			map.remove("schemaType");
			map.remove("tags");
			map.remove("createdDate");
			map.remove("updatedDate");
			topic.setUpdatedBy(topicDTO.did());
			topic.setUpdatedDate(LocalDateTime.now());
			BeanUtils.copyProperties(topic, map);
			topic.setId(_topic.getId());
			topic.setName(_topic.getName());
			topic.setOwner(_topic.getOwner());
			topic.setSchemaType(_topic.getSchemaType());
			topic.setTags(topicDTO.getTags());
			topic.setCreatedBy(_topic.getCreatedBy());
			topic.setCreatedDate(_topic.getCreatedDate());
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new MongoException("Unable to update");
		}

		topic.setUpdatedDate(LocalDateTime.now());
		update(topic);
	}

//	@CacheResult(cacheName = "topic")
	public List<String> validateTopicIds(@CacheKey List<String> topicIds) {
		
		topicIds.forEach(id -> {
			findByIdOptional(new ObjectId(id)).orElseThrow(() -> new MongoException("id:" + id + " not exists"));
		});

		return topicIds;
	}

//	@CacheInvalidateAll(cacheName = "topic")
//	@CacheInvalidateAll(cacheName = "tversion")
	public void deleteTopic(String id) {
		try {
			deleteById(new ObjectId(id));
			topicVersionRepository.delete("topicId", new ObjectId(id));
		} catch (Exception e) {
			throw new MongoException("Unable to delete");
		}
	}

//	@CacheResult(cacheName = "topic")
	public TopicDTOPage queryByOwnerNameTags(@CacheKey String owner, @CacheKey String name, @CacheKey int page,
			@CacheKey int size, @CacheKey String... tags) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		StringBuffer buffer = new StringBuffer("owner = ?1");
		Optional.ofNullable(name).ifPresent(value -> {
			if (!value.isEmpty()) {
				buffer.append(" and name = ?2");
			}
		});

		Optional.ofNullable(tags).ifPresent(value -> {
			if (value.length > 0) {
				buffer.append(" and tags in ?3");
			}
		});

		long totalRecord = find(buffer.toString(), owner, name, tags).count();

		PanacheQuery<Topic> topics = find(buffer.toString(), owner, name, tags);
		if (size > 0) {
			topics.page(Page.of(page - 1, size));
		}
		topics.list().forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				map.remove("tags");
				map.remove("createdDate");
				map.remove("updatedDate");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setUpdatedDate(entity.getUpdatedDate());
				topicDTO.setCreatedDate(entity.getCreatedDate());
				topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()).name());
				topicDTO.setTags(entity.getTags());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return new TopicDTOPage(totalRecord, size == 0 ? totalRecord : size, page, topicDTOs);
	}

//	@CacheResult(cacheName = "topic")
	public HashMap<String, Integer> countByOwner(@CacheKey String[] owner) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		PanacheQuery<Topic> topics = find("owner in ?1", List.of(owner));

		HashMap<String, Integer> topicOwner = new HashMap();
		topics.list().forEach(entity -> {
			if (topicOwner.containsKey(entity.getOwner())) {
				topicOwner.put(entity.getOwner(), topicOwner.get(entity.getOwner()) + 1);
			} else {
				topicOwner.put(entity.getOwner(), 1);
			}
		});
		return topicOwner;
	}

//	@CacheResult(cacheName = "topic")
	public TopicDTOPage queryByOwnerOrName(@CacheKey String keyword, @CacheKey String owner, @CacheKey int page,
			@CacheKey int size) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		StringBuffer buffer = new StringBuffer("name like ?1");
		Optional.ofNullable(owner).ifPresent(value -> {
			if (!value.isEmpty()) {
				buffer.append(" and owner = ?2");
			}
		});

		long totalRecord = find(buffer.toString(), keyword, owner).count();

		PanacheQuery<Topic> topics = find(buffer.toString(), keyword, owner);
		if (size > 0) {
			topics.page(Page.of(page - 1, size));
		}
		topics.list().forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				map.remove("tags");
				map.remove("createdDate");
				map.remove("updatedDate");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setUpdatedDate(entity.getUpdatedDate());
				topicDTO.setCreatedDate(entity.getCreatedDate());
				topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()).name());
				topicDTO.setTags(entity.getTags());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return new TopicDTOPage(totalRecord, size == 0 ? totalRecord : size, page, topicDTOs);
	}

//	@CacheResult(cacheName = "topic")
	public TopicDTO findTopicBy(@CacheKey String id,@CacheKey String versionNumber) {
		TopicDTO topicDTO = new TopicDTO();
		try {
			Topic entity = findById(new ObjectId(id));
			// if(entity.getVersion().equalsIgnoreCase(versionNumber)) {
			// throw new MongoException("id:" + id + " version " + versionNumber + "
			// exists");
			// }
			Map map = BeanUtils.describe(entity);
			map.remove("schemaType");
			map.remove("tags");
			map.remove("createdDate");
			map.remove("updatedDate");
			BeanUtils.copyProperties(topicDTO, map);
			topicDTO.setUpdatedDate(entity.getUpdatedDate());
			topicDTO.setCreatedDate(entity.getCreatedDate());
			topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()).name());
			topicDTO.setTags(entity.getTags());
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
		}
		return topicDTO;
	}

//	@CacheInvalidateAll(cacheName = "topic")
//	@CacheInvalidateAll(cacheName = "tversion")
	public void deleteTopic(String id, String version) {
		long totaltopic = topicVersionRepository.find("topicId = ?1", new ObjectId(id)).count();
		if (totaltopic == 1) {
			deleteTopic(id);
		} else {
			topicVersionRepository.delete("topicId = ?1 and version = ?2", new ObjectId(id), version);
		}
	}

//	@CacheInvalidateAll(cacheName = "topic")
//	@CacheInvalidateAll(cacheName = "tversion")
	public TopicDTO updateByIdAndVersion(String id,
			String versionNumber,
			String schema, String did) {
		TopicDTO topicDTO = findTopicBy(id, versionNumber);
		TopicDTO _topicDTO = topicVersionRepository.updateByIdAndVersion(id, versionNumber, schema, did);
		topicDTO.setUpdatedDate(_topicDTO.getUpdatedDate());
		topicDTO.setSchema(schema);
		topicDTO.setVersion(versionNumber);
		return topicDTO;
	}

}
