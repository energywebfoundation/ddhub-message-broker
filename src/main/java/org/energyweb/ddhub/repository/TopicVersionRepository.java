package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTOPage;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.MongoException;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class TopicVersionRepository implements PanacheMongoRepository<TopicVersion> {

	@Inject
	TopicRepository topicRepository;

//	@CacheResult(cacheName = "tversion")
	public TopicDTO findByIdAndVersion(@CacheKey String id,@CacheKey String versionNumber) {
		try {
			TopicVersion topicVersion = find("topicId = ?1 and version = ?2 and deleted is null or deleted = ?3",Sort.descending("_id"), new ObjectId(id), versionNumber, false)
					.firstResultOptional().orElseThrow(() -> new MongoException("id:" + id + " version not exists"));

			Topic topic = topicRepository.findById(new ObjectId(id));

			Map map = BeanUtils.describe(topicVersion);
			map.remove("schemaType");
			map.remove("id");
			map.remove("createdDate");
			map.remove("updatedDate");
			map.remove("deletedDate");
			TopicDTO topicDTO = new TopicDTO();
			BeanUtils.copyProperties(topicDTO, topic);
			BeanUtils.copyProperties(topicDTO, map);
			topicDTO.setUpdatedDate(topicVersion.getUpdatedDate());
			topicDTO.setCreatedDate(topicVersion.getCreatedDate());
			topicDTO.setDeletedDate(topicVersion.getDeletedDate());
			topicDTO.setSchema(topicVersion.getSchema());
			return topicDTO;
		} catch (IllegalAccessException | InvocationTargetException | MongoException | NoSuchMethodException e) {
			throw new MongoException("id:" + id + " version not exists");
		}
	}

	public void validateByIdAndVersion(String id, String versionNumber) {
		findByIdAndVersion(id, versionNumber);
	}

//	@CacheResult(cacheName = "tversion")
	public TopicDTOPage findListById(@CacheKey String id,@CacheKey int page,@CacheKey int size,@CacheKey boolean includeDeleted,@CacheKey LocalDateTime from) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		long totalRecord = find("topicId = ?1", new ObjectId(id)).count();

		Topic topic = topicRepository.findById(new ObjectId(id));
		
		StringBuffer buffer = new StringBuffer("topicId = ?1");
		Optional.ofNullable(includeDeleted).ifPresent(value -> {
			if(!value) {
				buffer.append(" and deleted is null or deleted = ?2");
			}
		});
		
		Optional.ofNullable(from).ifPresent(value -> {
			buffer.append(" and updatedDate > ?3");
		});

		PanacheQuery<TopicVersion> topics = find(buffer.toString(), new ObjectId(id),includeDeleted,from);
		if (size > 0) {
			topics.page(Page.of(page - 1, size));
		}
		topics.list().forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				map.remove("id");
				map.remove("createdDate");
				map.remove("updatedDate");
				map.remove("deletedDate");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, topic);
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setUpdatedDate(entity.getUpdatedDate());
				topicDTO.setCreatedDate(entity.getCreatedDate());
				topicDTO.setDeletedDate(entity.getDeletedDate());
				topicDTO.setSchema(entity.getSchema());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return new TopicDTOPage(totalRecord, size == 0 ? totalRecord : size, page, topicDTOs);
	}
	
	
//	@CacheInvalidateAll(cacheName = "tversion")
//	@CacheInvalidateAll(cacheName = "topic")
	public TopicDTO updateByIdAndVersion(String id, String versionNumber, String schema, String did) {
		TopicDTO topicDTO = new TopicDTO();
		topicDTO.setSchema(schema);
		topicDTO.setVersion(versionNumber);

		TopicVersion topicVersion = find("topicId = ?1 and version = ?2 and deleted is null or deleted = ?3", new ObjectId(id), versionNumber,false)
				.firstResultOptional().orElse(new TopicVersion());
		topicVersion.setSchema(schema);
		topicVersion.setUpdatedBy(did);
		topicVersion.setUpdatedDate(LocalDateTime.now());
		if (topicVersion.getId() == null) {
			topicVersion.setTopicId(new ObjectId(id));
			topicVersion.setVersion(versionNumber);
			topicVersion.setCreatedBy(did);
			topicVersion.setCreatedDate(LocalDateTime.now());
		}
		persistOrUpdate(topicVersion);
		topicDTO.setUpdatedDate(topicVersion.getUpdatedDate());
		return topicDTO;
	}

}
