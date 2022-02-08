package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;

import io.nats.client.JetStreamApiException;

@Provider
public class NatsExceptionMapper implements ExceptionMapper<JetStreamApiException> {
	@Override
	public Response toResponse(final JetStreamApiException exception) {
		return Response.status(400).entity(new ErrorResponse("20", exception.getErrorDescription())).build();
	}
}
