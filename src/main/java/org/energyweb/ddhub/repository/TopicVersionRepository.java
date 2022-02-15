package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.model.TopicVersion;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TopicVersionRepository implements PanacheMongoRepository<TopicVersion> {
    public TopicDTO findByIdAndVersion(String id, String versionNumber) {
    	try {
    		TopicVersion topicVersion = find("topicId = ?1 and version = ?2", new ObjectId(id), versionNumber).firstResultOptional().orElseThrow(()-> new MongoException("id:" + id + " not exists"));
    		TopicDTO topicDTO = new TopicDTO();
			BeanUtils.copyProperties(topicDTO, topicVersion);
			return topicDTO;
		} catch (IllegalAccessException | InvocationTargetException | MongoException e) {
			throw new MongoException("id:" + id + " not exists");
		}
    }

    public List<TopicDTO> findListById(String id) {
    	List<TopicDTO> topicDTOs = new ArrayList<>();
    	list("topicId", new ObjectId(id)).forEach(entity -> {
			try {
				TopicDTO topicDTO = new TopicDTO();
				BeanUtils.copyProperties(topicDTO, entity);
				topicDTOs.add(topicDTO);
			} catch (IllegalAccessException | InvocationTargetException e) {
			}
		});
        return topicDTOs;
    }
}
