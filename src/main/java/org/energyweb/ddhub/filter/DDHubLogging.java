package org.energyweb.ddhub.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

@Provider
@Priority(Priorities.USER)
public class DDHubLogging implements ContainerRequestFilter {

	@Inject
	Logger logger;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		String json = new String(Base64.getUrlDecoder().decode(authorizationHeader.split("\\.")[1]),
				StandardCharsets.UTF_8);
		JSONParser parser = new JSONParser();
		try {
			
			ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(requestContext.getEntityStream().readAllBytes()); 
			requestContext.setEntityStream(arrayInputStream);
			
			
			String parameters = requestContext.hasEntity()? new String(arrayInputStream.readAllBytes(), "UTF-8"):JsonbBuilder.create().toJson(requestContext.getUriInfo().getQueryParameters());
			HashMap parametersObject = (JSONObject) parser.parse(parameters);
			parametersObject.remove("payload");
			parametersObject.remove("schema");
			
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			HashMap<String, String> data = new HashMap<>(); 
			data.put("method", requestContext.getMethod());
			data.put("path", requestContext.getUriInfo().getPath());
			data.put("request", JsonbBuilder.create().toJson(parametersObject));
			
			this.logger.info("[" + jsonObject.get("did") + "]" + JsonbBuilder.create().toJson(data));

			requestContext.getEntityStream().reset();
		} catch (ParseException e) {
			this.logger.error(JsonbBuilder.create().toJson(e.getMessage()));
		}
	}

}
