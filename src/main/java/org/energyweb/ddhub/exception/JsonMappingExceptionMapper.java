package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonMappingException;

@Provider
public class JsonMappingExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<JsonMappingException> {
	@Inject
	Logger logger;

	@Override
	public Response toResponse(final JsonMappingException exception) {
		ErrorResponse error = new ErrorResponse("14", "Cannot parse JSON");
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
