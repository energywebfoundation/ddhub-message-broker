package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.beanutils.BeanUtils;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.model.Channel;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ChannelRepository implements PanacheMongoRepository<Channel> {

	public ChannelDTO findByFqcn(String fqcn) {
		try {
			ChannelDTO channelDTO = new ChannelDTO();
			Channel channel = find("fqcn", fqcn).firstResultOptional()
					.orElseThrow(() -> new MongoException("fqcn:" + fqcn + " not exists"));
			BeanUtils.copyProperties(channelDTO, channel);
			return channelDTO;
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("fqcn:" + fqcn + " not exists");
		}
	}

	public void deleteByFqcn(String fqcn) {
		delete("fqcn", fqcn);
	}

	public void validateChannel(String fqcn) {
		findByFqcn(fqcn);
	}

	public void updateChannel(@Valid @NotNull ChannelDTO channelDTO) {
		try {
			Channel channel = find("fqcn", channelDTO.getFqcn()).firstResultOptional()
					.orElseThrow(() -> new MongoException("fqcn:" + channelDTO.getFqcn() + " not exists"));
			BeanUtils.copyProperties(channel, channelDTO);
			channel.setUpdatedDate(LocalDateTime.now());
			update(channel);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to update");
		}
	}

	public void save(ChannelDTO channelDTO) {
		try {
			Channel channel = new Channel();
			BeanUtils.copyProperties(channel, channelDTO);
			channel.setCreatedDate(LocalDateTime.now());
			persist(channel);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to save");
		}

	}

	public List<ChannelDTO> listChannel(String ownerDID) {
		List<ChannelDTO> channelDTOs = new ArrayList<>();
		list("ownerdid", ownerDID).forEach(entity -> {
			try {
				ChannelDTO channelDTO = new ChannelDTO();
				BeanUtils.copyProperties(channelDTO, entity);
				channelDTOs.add(channelDTO);
			} catch (IllegalAccessException | InvocationTargetException e) {
			}
		});
		return channelDTOs;
	}

	// public void validateChannel(String fqcn, String topicId, String ownerId) {
	// ChannelDTO channelDTO = findByFqcn(fqcn);
	// Optional.ofNullable(channelDTO).filter(dto->dto.getTopicIds().contains(topicId)).orElseThrow(()->new
	// MongoException("topicId:" + topicId + " not exists for channel " + fqcn));
	// Optional.ofNullable(channelDTO).filter(dto->dto.getOwnerdid().contentEquals(ownerId)).orElseThrow(()->new
	// MongoException("Unauthorized access"));
	// }

}
