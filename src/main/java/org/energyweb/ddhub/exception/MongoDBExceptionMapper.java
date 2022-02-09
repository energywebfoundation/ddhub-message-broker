package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;

import com.mongodb.MongoException;

@Provider
public class MongoDBExceptionMapper implements ExceptionMapper<MongoException> {
	@Override
	public Response toResponse(final MongoException exception) {
		return Response.status(400).entity(new ErrorResponse("50", exception.getMessage())).build();
	}
}
