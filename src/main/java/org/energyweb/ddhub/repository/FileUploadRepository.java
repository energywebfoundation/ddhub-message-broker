package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;

import org.apache.commons.beanutils.BeanUtils;
import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.FileUploadDTO;
import org.energyweb.ddhub.model.FileUpload;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class FileUploadRepository implements PanacheMongoRepository<FileUpload> {

	public String save(@Valid FileUploadDTO data, ChannelDTO channelDTO) {
		try {
			FileUpload fileUpload = new FileUpload();
			BeanUtils.copyProperties(fileUpload, data);
			BeanUtils.copyProperties(fileUpload, channelDTO);
			fileUpload.setCreatedDate(LocalDateTime.now());
			persist(fileUpload);
			return fileUpload.getId().toString();
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to save");
		}
	}

	public MessageDTO findByFileId(String fileId) {
		FileUpload fileUpload = find("_id", new ObjectId(fileId)).firstResultOptional().orElseThrow(() -> new MongoException("id:" + fileId + " not exists"));
		if(fileUpload.validateExpiryContent()) {
			throw new MongoException("File are expired");
		}
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setFqcn(fileUpload.getFqcn());
		messageDTO.setTopicId(fileUpload.getTopicId());
		messageDTO.setSenderDid(fileUpload.getOwnerdid());
		messageDTO.setSignature(fileUpload.getSignature());
		messageDTO.setClientGatewayMessageId(fileUpload.getClientGatewayMessageId());
		messageDTO.setPayloadEncryption(fileUpload.isPayloadEncryption());
		return messageDTO;
	}
	
	public String findFilenameByFileId(String fileId) {
		FileUpload fileUpload = find("_id", new ObjectId(fileId)).firstResultOptional().orElseThrow(() -> new MongoException("id:" + fileId + " not exists"));
		return fileUpload.getFileName();
	}
   
   
}
