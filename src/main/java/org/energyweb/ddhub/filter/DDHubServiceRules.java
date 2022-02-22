package org.energyweb.ddhub.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
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
		String json = new String(Base64.getUrlDecoder().decode(authorizationHeader.split("\\.")[1]), StandardCharsets.UTF_8);
		JSONParser parser = new JSONParser();  
		
    	try {
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			JSONArray jsonArray =  (JSONArray) jsonObject.get("verifiedRoles");
			if(jsonArray.toJSONString().indexOf(rulesConfig.ddhubNamespace()) < 0) {
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("10", "Unauthorize access")).build());
			}
			jsonArray.forEach(item -> {
			    JSONObject obj = (JSONObject) item;
			    String namespace = (String) obj.get("namespace");
			    if(namespace.indexOf(rulesConfig.ddhubNamespace()) < 0) {
			    	Optional<DDHubServiceRulesConfig.DDHubService> hubService = rulesConfig.services().stream().filter(s->s.method().contentEquals(requestContext.getMethod()) && s.path().contentEquals(requestContext.getUriInfo().getPath())).findFirst();
			    	hubService.ifPresent(service ->{
			    		if(service.rules().stream().filter(str->namespace.matches(str.replace("*", "(\\w*)"))).findFirst().isEmpty()) {
			    			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("10", "Unauthorize access")).build());
			    		}
			    	});
			    }
			});
			
		} catch (ParseException e) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("10", "Unauthorize access")).build());
		}  	 
	}

}
