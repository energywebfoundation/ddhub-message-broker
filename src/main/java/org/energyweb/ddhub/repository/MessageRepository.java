package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalLong;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.model.Message;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class MessageRepository implements PanacheMongoRepository<Message> {
	
    @ConfigProperty(name = "DUPLICATE_WINDOW")
    OptionalLong duplicateWindow;

	public String save(MessageDTO messageDTO, String did) {
		Message message = new Message();
		message.setSenderdid(did);
		if(Optional.ofNullable(messageDTO.getTransactionId()).isEmpty()) {
			saveMessage(messageDTO, message);
		}else {
			PanacheQuery<Message> _message = find("transactionId = ?1 and senderdid = ?2 and fqcn = ?3 and topicId = ?4", Sort.by("_id").descending(), 
					messageDTO.getTransactionId(),did,messageDTO.getFqcn(),messageDTO.getTopicId(),
					messageDTO.getTopicVersion());
			if(_message.firstResultOptional().isPresent()) {
				Message __message = _message.firstResultOptional().get();
				if(__message.getCreatedDate() != null && !__message.getCreatedDate().isAfter(LocalDateTime.now().minusSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW)))) {
					saveMessage(messageDTO, message);
				}else {
					message = __message;
				}
			}else {
				saveMessage(messageDTO, message);
			}
		}
		return message.getId().toString();
	}

	private void saveMessage(MessageDTO messageDTO, Message message) {
		try {
			BeanUtils.copyProperties(message, messageDTO);
			message.setCreatedDate(LocalDateTime.now());
		} catch (IllegalAccessException | InvocationTargetException e) {
		}
		persist(message);
	}
}
