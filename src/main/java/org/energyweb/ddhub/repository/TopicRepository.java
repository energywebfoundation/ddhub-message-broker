package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

	// @CacheInvalidateAll(cacheName = "topic")
	// @CacheInvalidateAll(cacheName = "tversion")
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

	// @CacheInvalidateAll(cacheName = "topic")
	// @CacheInvalidateAll(cacheName = "tversion")
	public void updateTopic(TopicDTO topicDTO) {
		Topic topic = new Topic();
		topic.setUpdatedBy(topicDTO.did());
		topic.setUpdatedDate(LocalDateTime.now());
		topic.setId(new ObjectId(topicDTO.getId()));
		topic.setName(topicDTO.getName());
		topic.setOwner(topicDTO.getOwner());
		topic.setSchemaType(topicDTO.getSchemaType());
		topic.setTags(topicDTO.getTags());
		topic.setCreatedBy(topicDTO.getCreatedBy());
		topic.setCreatedDate(topicDTO.getCreatedDate());
		update(topic);
	}

	public List<String> validateTopicIds(List<String> topicIds) {
		return validateTopicIds(topicIds,false);
	}
	
	// @CacheResult(cacheName = "topic")
	public List<String> validateTopicIds(@CacheKey List<String> topicIds,@CacheKey boolean includeDeleted) {
		StringBuffer buffer = new StringBuffer("_id in ?1");
		Optional.ofNullable(includeDeleted).ifPresent(value -> {
			if(!value) {
				buffer.append(" and deleted is null or deleted = ?2");
			}
		});
		
		List<Topic> result = find(buffer.toString(),
				topicIds.stream().map(id -> new ObjectId(id)).collect(Collectors.toList()),includeDeleted).list();
		if (result.size() != topicIds.size()) {
			List<String> _topicIds = result.stream().map(topic -> topic.getId().toHexString())
					.collect(Collectors.toList());
			throw new MongoException(
					"id: " + topicIds.stream().filter(id -> !_topicIds.contains(id)).collect(Collectors.toList())
							+ " not exists");
		}

		return topicIds;
	}

	// @CacheInvalidateAll(cacheName = "topic")
	// @CacheInvalidateAll(cacheName = "tversion")
	public void deleteTopic(String id) {
		try {
			update("deletedDate = ?1 and deleted = ?2", LocalDateTime.now(),true).where("_id", new ObjectId(id));
			topicVersionRepository.update("deletedDate = ?1 and deleted = ?2", LocalDateTime.now(),true).where("topicId", new ObjectId(id));
		} catch (Exception e) {
			throw new MongoException("Unable to delete");
		}
	}

	// @CacheResult(cacheName = "topic")
	public TopicDTOPage queryByOwnerNameTags(@CacheKey String owner, @CacheKey String name, @CacheKey int page,
			@CacheKey int size, @CacheKey boolean includeDeleted,@CacheKey LocalDateTime from, @CacheKey String... tags) {
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

		Optional.ofNullable(includeDeleted).ifPresent(value -> {
			if(!value) {
				buffer.append(" and deleted is null or deleted = ?4");
			}
		});
		
		Optional.ofNullable(from).ifPresent(value -> {
			buffer.append(" and updatedDate > ?5");
		});


		long totalRecord = find(buffer.toString(), owner, name, tags,includeDeleted,from).count();

		PanacheQuery<Topic> topics = find(buffer.toString(), owner, name, tags, includeDeleted,from);
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
				map.remove("deletedDate");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setUpdatedDate(entity.getUpdatedDate());
				topicDTO.setCreatedDate(entity.getCreatedDate());
				topicDTO.setDeletedDate(entity.getDeletedDate());
				topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()).name());
				topicDTO.setTags(entity.getTags());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return new TopicDTOPage(totalRecord, size == 0 ? totalRecord : size, page, topicDTOs);
	}

	// @CacheResult(cacheName = "topic")
	public HashMap<String, Integer> countByOwner(@CacheKey String[] owner) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		PanacheQuery<Topic> topics = find("owner in ?1 and deleted is null or deleted = ?2", List.of(owner),false);

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

	// @CacheResult(cacheName = "topic")
	public TopicDTOPage queryByOwnerOrName(@CacheKey String keyword, @CacheKey String owner, @CacheKey int page,
			@CacheKey int size) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		StringBuffer buffer = new StringBuffer("name like ?1");
		Optional.ofNullable(owner).ifPresent(value -> {
			if (!value.isEmpty()) {
				buffer.append(" and owner = ?2");
			}
		});
		buffer.append(" and deleted is null or deleted = ?3");

		long totalRecord = find(buffer.toString(), keyword, owner, false).count();

		PanacheQuery<Topic> topics = find(buffer.toString(), keyword, owner, false);
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
				map.remove("deletedDate");
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

	// @CacheResult(cacheName = "topic")
	public TopicDTO findTopicBy(@CacheKey String id) {
		TopicDTO topicDTO = new TopicDTO();
		try {
			Topic entity = findById(new ObjectId(id));
			Map map = BeanUtils.describe(entity);
			map.remove("schemaType");
			map.remove("tags");
			map.remove("createdDate");
			map.remove("updatedDate");
			map.remove("deletedDate");
			BeanUtils.copyProperties(topicDTO, map);
			topicDTO.setUpdatedDate(entity.getUpdatedDate());
			topicDTO.setCreatedDate(entity.getCreatedDate());
			topicDTO.setDeletedDate(entity.getDeletedDate());
			topicDTO.setSchemaType(SchemaType.valueOf(entity.getSchemaType()).name());
			topicDTO.setTags(entity.getTags());
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
		}
		return topicDTO;
	}

	// @CacheInvalidateAll(cacheName = "topic")
	// @CacheInvalidateAll(cacheName = "tversion")
	public void deleteTopic(String id, String version) {
		long totaltopic = topicVersionRepository.find("topicId = ?1 and deleted = ?2", new ObjectId(id),false).count();
		if (totaltopic == 1) {
			deleteTopic(id);
		} else {
			topicVersionRepository.update("deletedDate = ?1 and deleted = ?2", LocalDateTime.now(),true)
					.where("topicId = ?1 and version = ?2 and deleted = ?3", new ObjectId(id), version,false);
		}
	}

	// @CacheInvalidateAll(cacheName = "topic")
	// @CacheInvalidateAll(cacheName = "tversion")
	public TopicDTO updateByIdAndVersion(String id,
			String versionNumber,
			String schema, String did) {
		TopicDTO topicDTO = findTopicBy(id);
		TopicDTO _topicDTO = topicVersionRepository.updateByIdAndVersion(id, versionNumber, schema, did);
		topicDTO.setUpdatedDate(_topicDTO.getUpdatedDate());
		topicDTO.setSchema(schema);
		topicDTO.setVersion(versionNumber);
		return topicDTO;
	}

}
