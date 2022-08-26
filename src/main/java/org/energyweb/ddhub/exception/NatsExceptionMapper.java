package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

import io.nats.client.JetStreamApiException;

@Provider
public class NatsExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<JetStreamApiException> {
	@Inject
	Logger logger;

	@Override
	public Response toResponse(final JetStreamApiException exception) {
		ErrorResponse error = new ErrorResponse("20", exception.getErrorDescription());
		this.logger.error("[" + userDid() + "][" + requestId() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
