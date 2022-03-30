package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

@Provider
public class JsonbParseExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<JsonbException> {
	@Inject
	Logger logger;

	@Override
	public Response toResponse(final JsonbException exception) {
		ErrorResponse error = new ErrorResponse("13", exception.getMessage());
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
