package org.energyweb.ddhub.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.model.TopicMonitor;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;

@ApplicationScoped
public class TopicMonitorRepository implements PanacheMongoRepository<TopicMonitor> {

	@CacheResult(cacheName = "topic-monitor")
	public List<TopicMonitor>  findLatestUpdateBy(@CacheKey String[] namespaces) {
		PanacheQuery<TopicMonitor> topics = find("owner in ?1", List.of(namespaces));
		return topics.list();
	}

	@CacheInvalidateAll(cacheName = "topic-monitor")
	public void topicUpdatedBy(String owner) {
		try {
			update("lastTopicUpdate = ?1", TimeUnit.NANOSECONDS.toNanos(new Date().getTime())).where("owner", owner);
		} catch (Exception e) {
		}
	}
	
	@CacheInvalidateAll(cacheName = "topic-monitor")
	public void topicVersionUpdatedBy(String owner) {
		try {
			update("lastTopicVersionUpdate = ?1", TimeUnit.NANOSECONDS.toNanos(new Date().getTime())).where("owner", owner);
		} catch (Exception e) {
		}
	}

	@CacheInvalidateAll(cacheName = "topic-monitor")
	public void createBy(String owner) {
		try {
			persist(new TopicMonitor(owner));
		} catch (Exception e) {
			topicUpdatedBy(owner);
			topicVersionUpdatedBy(owner);
		}
	}

	@CacheInvalidateAll(cacheName = "topic-monitor")
	public void populateData(List<Topic> topics) {
		topics = topics.stream().filter(distinctByKey(p -> p.getOwner())).collect( Collectors.toList());
		topics.forEach(topic -> {
			persist(new TopicMonitor(topic.getOwner()));
		});
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) 
	{
	  Map<Object, Boolean> map = new ConcurrentHashMap<>();
	  return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}
}
