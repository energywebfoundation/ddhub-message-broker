package org.energyweb.ddhub.exception;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

@Provider
public class ValidationExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<ValidationException> {
	@Inject
	Logger logger;

	@Override
	public Response toResponse(ValidationException exception) {
		if (exception instanceof ResteasyViolationException) {
			List<ResteasyConstraintViolation> parameters = ((ResteasyViolationException) exception)
					.getParameterViolations();
			String messages = "";
			Iterator<ResteasyConstraintViolation> it = parameters.iterator();
			while (it.hasNext()) {
				ResteasyConstraintViolation constraintViolation = (ResteasyConstraintViolation) it.next();
				String[] parameter = constraintViolation.getPath().split("\\.");
				messages += parameter[parameter.length - 1] + " : " + constraintViolation.getMessage();
				if (it.hasNext())
					messages += ", ";
			}

			ErrorResponse error = new ErrorResponse("11", messages);
			this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
			return Response.status(400).entity(error).build();
		}

		ErrorResponse error = new ErrorResponse("11", exception.getMessage());
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}

}