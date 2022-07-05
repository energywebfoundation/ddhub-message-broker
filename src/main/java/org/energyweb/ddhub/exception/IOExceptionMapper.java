package org.energyweb.ddhub.exception;

import java.io.IOException;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

import com.mongodb.MongoException;

@Provider
public class IOExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<IOException> {

	@Inject
	Logger logger;

	@Override
	public Response toResponse(final IOException exception) {
		ErrorResponse error = new ErrorResponse("60", exception.getMessage());
		if (error.getReturnMessage().contains("Error Publishing")) {
			error.setReturnCode("21");
			error.setReturnMessage("FQCN not exists");
		}
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		int httpCode = 400;
		if (error.getReturnMessage().contains("not exists")) {
			httpCode = 404;
		}
		return Response.status(httpCode).entity(error).build();
	}
}
