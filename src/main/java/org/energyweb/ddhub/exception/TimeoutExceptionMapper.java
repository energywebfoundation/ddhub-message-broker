package org.energyweb.ddhub.exception;

import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

@Provider
public class TimeoutExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<TimeoutException> {

	@Inject
	Logger logger;

	@Override
	public Response toResponse(final TimeoutException exception) {
		ErrorResponse error = new ErrorResponse("60", "TimeoutException");
		this.logger.error("[" + userDid() + "][" + requestId() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}
}
