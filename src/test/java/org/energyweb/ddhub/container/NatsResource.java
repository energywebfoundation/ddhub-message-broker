package org.energyweb.ddhub.container;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class NatsResource implements QuarkusTestResourceLifecycleManager {
	public static final String NATS_IMAGE_NAME = "nats:latest";
    public static final int NATS_PORT = 4222;

    private GenericContainer<?> natsContainer;

    @Override
    public Map<String, String> start() {
    	natsContainer = new GenericContainer<>(NATS_IMAGE_NAME).withCommand("-js").withExposedPorts(NATS_PORT);
        natsContainer.waitingFor(new HostPortWaitStrategy()).start();
        Map<String, String> properties = new HashMap<>();
        properties.put("%test.NATS_JS_URL","nats://"+natsContainer.getHost()+":"+ natsContainer.getMappedPort(NATS_PORT));
        return properties;
    }

    @Override
    public void stop() {
    	natsContainer.stop();
    }
}
