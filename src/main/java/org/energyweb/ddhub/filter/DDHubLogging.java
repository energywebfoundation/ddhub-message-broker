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
	
	private static final String DEFAULT_CHARSET = "UTF-8";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		String requestId = requestContext.getHeaderString("X-Request-Id");
		String json = new String(Base64.getUrlDecoder().decode(authorizationHeader.split("\\.")[1]),
				StandardCharsets.UTF_8);
		JSONParser parser = new JSONParser();
		try {

			ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(
					requestContext.getEntityStream().readAllBytes());
			requestContext.setEntityStream(arrayInputStream);

			String parameters = requestContext.hasEntity() ? new String(arrayInputStream.readAllBytes(), "UTF-8")
					: JsonbBuilder.create().toJson(requestContext.getUriInfo().getQueryParameters());

			JSONObject jsonObject = (JSONObject) parser.parse(json);
			HashMap<String, String> data = new HashMap<>();
			data.put("method", requestContext.getMethod());
			data.put("path", requestContext.getUriInfo().getPath());
			data.put("X-Request-Id", requestId);

			try {
				HashMap parametersObject = (JSONObject) parser.parse(parameters);
				data.put("payloadSize", bytesIntoHumanReadable(
						JsonbBuilder.create().toJson(parametersObject.get("payload")).toCharArray().length));
				data.put("totalSize",
						bytesIntoHumanReadable(JsonbBuilder.create().toJson(parametersObject).toCharArray().length));
				parametersObject.remove("payload");
				parametersObject.remove("schema");
				data.put("request", JsonbBuilder.create().toJson(parametersObject));
			} catch (ParseException e) {
			    if(requestContext.getUriInfo().getPath().contains("upload")) {
			    	data.put("request", "--form-data--");
			    }else {
			    	data.put("request", JsonbBuilder.create().toJson(parameters));
			    	
			    }

			}

			this.logger.info("[" + jsonObject.get("did") + "][" + requestId + "]" + JsonbBuilder.create().toJson(data));
			data.clear();
			arrayInputStream.close();
			requestContext.getEntityStream().reset();
		} catch (ParseException e) {
			this.logger.error(JsonbBuilder.create().toJson(e.getMessage()));
		}
	}
	
	private String bytesIntoHumanReadable(long bytes) {
		long kilobyte = 1024;
		long megabyte = kilobyte * 1024;
		long gigabyte = megabyte * 1024;
		long terabyte = gigabyte * 1024;

		if ((bytes >= 0) && (bytes < kilobyte)) {
			return bytes + " B";

		} else if ((bytes >= kilobyte) && (bytes < megabyte)) {
			return (bytes / kilobyte) + " KB";

		} else if ((bytes >= megabyte) && (bytes < gigabyte)) {
			return (bytes / megabyte) + " MB";

		} else if ((bytes >= gigabyte) && (bytes < terabyte)) {
			return (bytes / gigabyte) + " GB";

		} else if (bytes >= terabyte) {
			return (bytes / terabyte) + " TB";

		} else {
			return bytes + " Bytes";
		}
	}

}
