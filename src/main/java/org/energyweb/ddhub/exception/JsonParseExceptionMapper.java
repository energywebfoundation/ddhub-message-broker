package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonParseException;

@Provider
public class JsonParseExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<JsonParseException> {
	@Inject
	Logger logger;

	@Override
	public Response toResponse(final JsonParseException exception) {
		ErrorResponse error = new ErrorResponse("15", "Cannot parse JSON");
		this.logger.error("[" + userDid() + "][" + requestId() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
