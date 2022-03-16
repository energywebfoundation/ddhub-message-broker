package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

@Provider
public class IllegalArgumentExceptionMapper extends DDHubHeaderMapper
		implements ExceptionMapper<IllegalArgumentException> {

	@Inject
	Logger logger;

	@Override
	public Response toResponse(IllegalArgumentException exception) {
		ErrorResponse error = new ErrorResponse("15", exception.getMessage());
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
