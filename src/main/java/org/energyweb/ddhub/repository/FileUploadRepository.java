package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.MultipartBody;
import org.energyweb.ddhub.model.FileUpload;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class FileUploadRepository implements PanacheMongoRepository<FileUpload> {

	public String save(@Valid MultipartBody data, ChannelDTO channelDTO) {
		try {
			FileUpload fileUpload = new FileUpload();
			BeanUtils.copyProperties(fileUpload, data);
			BeanUtils.copyProperties(fileUpload, channelDTO);
			persist(fileUpload);
			return fileUpload.getId().toString();
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to save");
		}
	}

	public MessageDTO findByFileId(String fileId) {
		FileUpload fileUpload = find("_id", new ObjectId(fileId)).firstResultOptional().orElseThrow(() -> new MongoException("id:" + fileId + " not exists"));
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setFqcn(fileUpload.getFqcn());
		messageDTO.setTopicId(fileUpload.getTopicId());
		return messageDTO;
	}
	
	public String findFilenameByFileId(String fileId) {
		FileUpload fileUpload = find("_id", new ObjectId(fileId)).firstResultOptional().orElseThrow(() -> new MongoException("id:" + fileId + " not exists"));
		return fileUpload.getFileName();
	}
   
   
}
