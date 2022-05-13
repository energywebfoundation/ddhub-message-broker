package org.energyweb.ddhub.exception;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;

import com.mongodb.MongoException;

@Provider
public class MongoDBExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<MongoException> {

	@Inject
	Logger logger;

	@Override
	public Response toResponse(final MongoException exception) {
		ErrorResponse error = new ErrorResponse("50", exception.getMessage());
		if (error.getReturnMessage().contains("E11000")) {
			error = new ErrorResponse("51", "Record exists");
		}
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		int httpCode = 400;
		if(error.getReturnMessage().contains("not exists")) {
			httpCode = 404;
		}
		return Response.status(httpCode).entity(error).build();
	}
}
