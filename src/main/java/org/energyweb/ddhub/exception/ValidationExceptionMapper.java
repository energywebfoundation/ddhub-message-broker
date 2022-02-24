package org.energyweb.ddhub.exception;

import java.util.Iterator;
import java.util.List;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

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

			return Response.status(400).entity(new ErrorResponse("11", messages)).build();
		}

		return Response.status(400).entity(new ErrorResponse("11", exception.getMessage())).build();
	}

}