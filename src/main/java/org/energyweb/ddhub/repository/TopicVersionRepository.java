package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTO.SchemaType;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TopicVersionRepository implements PanacheMongoRepository<TopicVersion> {
    public TopicDTO findByIdAndVersion(String id, String versionNumber) {
    	try {
    		TopicVersion topicVersion = find("topicId = ?1 and version = ?2", new ObjectId(id), versionNumber).firstResultOptional().orElseThrow(()-> new MongoException("id:" + id + " version not exists"));
			
			Map map = BeanUtils.describe(topicVersion);
			map.remove("schemaType");
    		TopicDTO topicDTO = new TopicDTO();
			BeanUtils.copyProperties(topicDTO, map);
			topicDTO.setSchemaType(SchemaType.valueOf(topicVersion.getSchemaType()));
			
			return topicDTO;
		} catch (IllegalAccessException | InvocationTargetException | MongoException | NoSuchMethodException e) {
			throw new MongoException("id:" + id + " version not exists");
		}
    }
    
    public void validateByIdAndVersion(String id, String versionNumber) {
    	findByIdAndVersion(id, versionNumber);
	}

	public List<TopicDTO> findListById(String id, String ownerDID) {
		List<TopicDTO> topicDTOs = new ArrayList<>();
    	list("topicId = ?1 and owner = ?2", new ObjectId(id),ownerDID).forEach(entity -> {
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

	public TopicDTO findByIdAndVersion( String id, String versionNumber, String ownerDID) {
		try {
    		TopicVersion topicVersion = find("topicId = ?1 and version = ?2 and owner = ?3", new ObjectId(id), versionNumber, ownerDID).firstResultOptional().orElseThrow(()-> new MongoException("id:" + id + " not exists"));
    		Map map = BeanUtils.describe(topicVersion);
			map.remove("schemaType");
    		TopicDTO topicDTO = new TopicDTO();
			BeanUtils.copyProperties(topicDTO, map);
			topicDTO.setSchemaType(SchemaType.valueOf(topicVersion.getSchemaType()));
			return topicDTO;
		} catch (IllegalAccessException | InvocationTargetException | MongoException | NoSuchMethodException e) {
			throw new MongoException("id:" + id + " not exists");
		}
	}

	
}
