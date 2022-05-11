package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

@Provider
public class JaxJsonParseExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<ProcessingException> {
	@Inject
	Logger logger;

	@Override
	public Response toResponse(final ProcessingException exception) {
		ErrorResponse error = new ErrorResponse("15", "Cannot parse JSON");
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
