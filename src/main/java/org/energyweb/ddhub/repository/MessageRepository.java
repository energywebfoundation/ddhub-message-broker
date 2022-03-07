package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.beanutils.BeanUtils;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.model.Message;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;

@ApplicationScoped
public class MessageRepository implements PanacheMongoRepository<Message> {

	public String save(MessageDTO messageDTO, String did) {
		Message message = new Message();
		message.setSenderdid(did);
		if(Optional.ofNullable(messageDTO.getTransactionId()).isEmpty()) {
			saveMessage(messageDTO, message);
		}else {
			PanacheQuery<Message> _message = find("transactionId = ?1 and senderdid = ?2 and fqcn = ?3 and topicId = ?4", 
					messageDTO.getTransactionId(),did,messageDTO.getFqcn(),messageDTO.getTopicId(),
					messageDTO.getTopicVersion());
			if(_message.firstResultOptional().isPresent()) {
				message = _message.firstResultOptional().get();
			}else {
				saveMessage(messageDTO, message);
			}
		}
		return message.getId().toString();
	}

	private void saveMessage(MessageDTO messageDTO, Message message) {
		try {
			BeanUtils.copyProperties(message, messageDTO);
		} catch (IllegalAccessException | InvocationTargetException e) {
		}
		persist(message);
	}
}
