package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.UnhandledException;

@Provider
public class UnhandledExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<UnhandledException> {

	@Inject
	Logger logger;

	@Override
	public Response toResponse(final UnhandledException exception) {
		ErrorResponse error = new ErrorResponse("60", exception.getMessage());
		this.logger.error("[" + userDid() + "][" + requestId() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
