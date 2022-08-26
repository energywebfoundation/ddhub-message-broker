package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.camel.CamelExecutionException;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

@Provider
public class CamelExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<CamelExecutionException> {
	@Inject
	Logger logger;
	
	@Override
	public Response toResponse(CamelExecutionException exception) {
		ErrorResponse error = new ErrorResponse("30", exception.getCause().getLocalizedMessage());
		this.logger.error("[" + userDid() + "][" + requestId() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
