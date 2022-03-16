package org.energyweb.ddhub.exception;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

public class DDHubHeaderMapper {

	@Context 
	HttpHeaders httpHeaders;
	
	protected String userDid() {
		try {
			String authorizationHeader = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0);
			String json = new String(Base64.getUrlDecoder().decode(authorizationHeader.split("\\.")[1]),
					StandardCharsets.UTF_8);
			JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
			return (String) jsonObject.get("did");
		} catch (ParseException e) {
			return null;
		}
	}

}