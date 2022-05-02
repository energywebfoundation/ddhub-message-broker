package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTOPage;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

@ApplicationScoped
public class TopicVersionRepository implements PanacheMongoRepository<TopicVersion> {

	@Inject
	TopicRepository topicRepository;

	public TopicDTO findByIdAndVersion(String id, String versionNumber) {
		try {
			TopicVersion topicVersion = find("topicId = ?1 and version = ?2", new ObjectId(id), versionNumber)
					.firstResultOptional().orElseThrow(() -> new MongoException("id:" + id + " version not exists"));

			Topic topic = topicRepository.findById(new ObjectId(id));

			Map map = BeanUtils.describe(topicVersion);
			map.remove("schemaType");
			map.remove("id");
			TopicDTO topicDTO = new TopicDTO();
			BeanUtils.copyProperties(topicDTO, topic);
			BeanUtils.copyProperties(topicDTO, map);
			topicDTO.setSchema(topicVersion.getSchema());
			return topicDTO;
		} catch (IllegalAccessException | InvocationTargetException | MongoException | NoSuchMethodException e) {
			throw new MongoException("id:" + id + " version not exists");
		}
	}

	public void validateByIdAndVersion(String id, String versionNumber) {
		findByIdAndVersion(id, versionNumber);
	}

	public TopicDTOPage findListById(String id, int page, int size) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
		long totalRecord = find("topicId = ?1", new ObjectId(id)).count();

		Topic topic = topicRepository.findById(new ObjectId(id));

		PanacheQuery<TopicVersion> topics = find("topicId = ?1", new ObjectId(id));
		if (size > 0) {
			topics.page(Page.of(page - 1, size));
		}
		topics.list().forEach(entity -> {
			try {
				Map map = BeanUtils.describe(entity);
				map.remove("schemaType");
				map.remove("id");
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, topic);
				BeanUtils.copyProperties(topicDTO, map);
				topicDTO.setSchema(entity.getSchema());
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			}
		});
		return new TopicDTOPage(totalRecord, size == 0 ? totalRecord : size, page, topicDTOs);
	}

	public TopicDTO updateByIdAndVersion(String id, String versionNumber, String schema, String did) {
		TopicDTO topicDTO = new TopicDTO();
		topicDTO.setSchema(schema);
		topicDTO.setVersion(versionNumber);

		TopicVersion topicVersion = find("topicId = ?1 and version = ?2", new ObjectId(id), versionNumber)
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

		return topicDTO;
	}

}
