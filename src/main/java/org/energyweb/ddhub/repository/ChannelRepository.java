package org.energyweb.ddhub.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.bson.types.ObjectId;
import org.energyweb.ddhub.dto.ChannelDTO;

import com.mongodb.MongoException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ChannelRepository implements PanacheMongoRepository<ChannelDTO> {

	public ChannelDTO findByFqcn(String fqcn) {
		return find("fqcn", fqcn).firstResult();
	}

	public void deleteByFqcn(String fqcn) {
		delete("fqcn", fqcn);
	}

}
