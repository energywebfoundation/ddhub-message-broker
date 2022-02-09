package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
			BeanUtils.copyProperties(channelDTO, find("fqcn", fqcn).firstResult());
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
			Channel channel = find("fqcn", channelDTO.getFqcn()).firstResult();
			BeanUtils.copyProperties(channel, channelDTO);
			update(channel);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to update");
		}
	}

	public void save(ChannelDTO channelDTO) {
		try {
			Channel channel = new Channel();
			BeanUtils.copyProperties(channel, channelDTO);
			persist(channel);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MongoException("Unable to save");
		}
		
	}

	public List<ChannelDTO> listChannel() {
		List<ChannelDTO> channelDTOs = new ArrayList<>();
		listAll().forEach(entity -> {
			try {
				ChannelDTO channelDTO = new ChannelDTO();
				BeanUtils.copyProperties(channelDTO, entity);
				channelDTOs.add(channelDTO);
			} catch (IllegalAccessException | InvocationTargetException e) {
			}
		});
		return channelDTOs;
	}

}
