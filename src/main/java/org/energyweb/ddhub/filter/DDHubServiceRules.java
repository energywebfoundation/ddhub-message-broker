package org.energyweb.ddhub.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import io.quarkus.security.Authenticated;

@Provider
@Priority(Priorities.AUTHORIZATION)
@Authenticated
public class DDHubServiceRules implements ContainerRequestFilter {

	@Inject
	Logger logger;

	@Inject
	DDHubServiceRulesConfig rulesConfig;

	@SuppressWarnings("unchecked")
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		String json = new String(Base64.getUrlDecoder().decode(authorizationHeader.split("\\.")[1]),
				StandardCharsets.UTF_8);
		JSONParser parser = new JSONParser();
		ErrorResponse error = new ErrorResponse("10", "Unauthorize access");
		try {
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			JSONArray jsonArray = (JSONArray) jsonObject.get("roles");
			if (Optional.ofNullable(jsonObject.get("did")).isEmpty()) {
				this.logger.error("missing did -> "
						+ authorizationHeader);
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
						.entity(error).build());
				return;
			}
			
			if (Optional.ofNullable(jsonArray).isEmpty()) {
				this.logger.error("[" + jsonObject.get("did") + "]" + " missing roles -> "
						+ authorizationHeader);
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
						.entity(error).build());
				return;
			}

			Optional<DDHubServiceRulesConfig.DDHubService> hubService = rulesConfig.services().stream()
					.sorted((DDHubServiceRulesConfig.DDHubService a1,
							DDHubServiceRulesConfig.DDHubService a2) -> a1.path().compareTo(a2.path()))
					.filter(s -> s.method().contentEquals(requestContext.getMethod())
							&& requestContext.getUriInfo().getPath()
									.matches(s.path().replaceAll("\\{[^{}]*}", "(\\\\w*.*)")))
					.findFirst();
			hubService.ifPresentOrElse(service -> {
				Set<String> ruleMatch = new HashSet<String>();
				
				List<String> result = service.rules().stream().filter(str -> !str.contains("*")).collect(Collectors.toList());
				result.forEach(rule->{
					if (jsonArray.stream().filter(str->str.toString().contains(rule)).findFirst().isEmpty()) {
						this.logger.error("[" + jsonObject.get("did") + "]" + "exact match rule " + rule + " not match " + jsonArray.toString());
						this.logger.error("[" + jsonObject.get("did") + "]" + JsonbBuilder.create().toJson(error));
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
								.entity(error).build());
						return;
					}else {
						ruleMatch.add(rule);
					}
				});
				
				jsonArray.forEach(item -> {
					String namespace = (String) item;
					if (!ruleMatch.contains(namespace) && service.rules().stream()
							.filter(str -> namespace.matches(str.replace("*", "(\\w*.*)")))
							.findFirst().isPresent()) {
						ruleMatch.add(namespace);
					}
				});
				
				
				if (service.rules().size() > ruleMatch.size()) {
					this.logger.error("[" + jsonObject.get("did") + "]" + JsonbBuilder.create().toJson(error));
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
							.entity(error).build());
					return;
				}

			}, () -> {
				this.logger.error("[" + jsonObject.get("did") + "]" + JsonbBuilder.create().toJson(error));
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(error).build());
			});
		} catch (ParseException e) {
			this.logger.error(JsonbBuilder.create().toJson(e.getMessage()));
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
					.entity(error).build());
		}
	}

}
