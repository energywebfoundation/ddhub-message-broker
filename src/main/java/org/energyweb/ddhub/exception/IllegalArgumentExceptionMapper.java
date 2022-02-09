package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.camel.CamelExecutionException;
import org.energyweb.ddhub.helper.ErrorResponse;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
	@Override
	public Response toResponse(IllegalArgumentException exception) {
		return Response.status(400).entity(new ErrorResponse("15", exception.getMessage())).build();
	}
}
