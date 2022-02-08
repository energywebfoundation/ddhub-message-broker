package org.energyweb.ddhub.exception;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.ArrayUtils;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

@Provider
public class PatternExceptionMapper implements ExceptionMapper<ResteasyViolationException> {
	@Override
	public Response toResponse(final ResteasyViolationException exception) {
		List<ResteasyConstraintViolation> parameters = ((ResteasyViolationException) exception)
				.getParameterViolations();
		String messages = "";
		Iterator<ResteasyConstraintViolation> it = parameters.iterator();
		while (it.hasNext()) {
			ResteasyConstraintViolation constraintViolation = (ResteasyConstraintViolation) it.next();
			String[] parameter = constraintViolation.getPath().split("\\.");
			if (parameter[parameter.length - 1].equals("<list element>")) {
				parameter = ArrayUtils.remove(parameter, parameter.length - 1);
			}

			messages += parameter[parameter.length - 1] + " : " + constraintViolation.getMessage();

			if (it.hasNext())
				messages += ", ";
		}

		return Response.status(400).entity(new ErrorResponse("20", messages)).build();
	}

}
