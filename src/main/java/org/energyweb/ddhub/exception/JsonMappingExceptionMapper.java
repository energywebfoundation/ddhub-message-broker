package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;

import com.fasterxml.jackson.databind.JsonMappingException;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
	@Override
	public Response toResponse(final JsonMappingException exception) {
		return Response.status(400).entity(new ErrorResponse("20", "Cannot parse JSON")).build();
	}
}
