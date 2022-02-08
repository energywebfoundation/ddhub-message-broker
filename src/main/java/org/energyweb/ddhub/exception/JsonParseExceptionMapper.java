package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;

import com.fasterxml.jackson.core.JsonParseException;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
	@Override
	public Response toResponse(final JsonParseException exception) {
		return Response.status(400).entity(new ErrorResponse("20", "Cannot parse JSON")).build();
	}
}
