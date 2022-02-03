package org.energyweb.ddhub.helper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.mongodb.MongoWriteException;

@Provider
public class MongoDBExceptionMapper implements ExceptionMapper<MongoWriteException> {
	@Override
	public Response toResponse(final MongoWriteException exception) {
		return Response.status(400).entity(new ErrorResponse("20", exception.getError().getMessage())).build();
	}
}
