package org.energyweb.ddhub.repository;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.beanutils.BeanUtils;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.helper.ReturnErrorMessage;
import org.energyweb.ddhub.helper.ReturnMessage;
import org.energyweb.ddhub.model.Channel;

import com.mongodb.MongoException;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ChannelRepository implements PanacheMongoRepository<Channel> {

	@CacheResult(cacheName = "channel")
	public ChannelDTO findByFqcn(@CacheKey String fqcn) {
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

	@CacheInvalidateAll(cacheName = "channel")
	public void deleteByFqcn(String fqcn) {
		delete("fqcn", fqcn);
	}

	public ReturnMessage validateChannel(String fqcn) {
		ReturnMessage errorMessage = null;
		try {
			findByFqcn(fqcn);
		}catch(MongoException ex) {
			errorMessage = new ReturnMessage();
			errorMessage.setDid(fqcn);
			errorMessage.setStatusCode(400);
			errorMessage.setErr(new ReturnErrorMessage("MB::INVALID_FQCN",ex.getMessage()));
		}
		return errorMessage;
	}

	@CacheInvalidateAll(cacheName = "channel")
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

}
