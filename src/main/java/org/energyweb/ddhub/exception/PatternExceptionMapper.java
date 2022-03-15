package org.energyweb.ddhub.exception;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.ArrayUtils;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.jboss.logging.Logger;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

@Provider
public class PatternExceptionMapper extends DDHubHeaderMapper implements ExceptionMapper<ResteasyViolationException> {

	@Inject
	Logger logger;

	@Context
	HttpHeaders httpHeaders;

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

		ErrorResponse error = new ErrorResponse("12", messages);
		this.logger.error("[" + userDid() + "]" + JsonbBuilder.create().toJson(error));
		return Response.status(400).entity(error).build();
	}

}
