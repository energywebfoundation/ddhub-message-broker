package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.camel.CamelExecutionException;
import org.energyweb.ddhub.helper.ErrorResponse;

@Provider
public class CamelExceptionMapper implements ExceptionMapper<CamelExecutionException> {
	@Override
	public Response toResponse(CamelExecutionException exception) {
		return Response.status(400).entity(new ErrorResponse("30", exception.getCause().getLocalizedMessage())).build();
	}
}
