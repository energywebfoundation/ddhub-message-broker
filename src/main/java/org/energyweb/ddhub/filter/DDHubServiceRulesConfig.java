package org.energyweb.ddhub.filter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

@StaticInitSafe
@ConfigMapping(prefix = "manage")
public interface DDHubServiceRulesConfig {
//	@WithDefault("dsb.apps.energyweb.ewc")
	Optional<String> ddhubNamespace();
	Set<DDHubService> services();
	
	interface DDHubService {
		String path();
		String method();
		List<String> rules();
		
	}
}
