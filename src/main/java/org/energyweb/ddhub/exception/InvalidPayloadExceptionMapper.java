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
public class InvalidPayloadExceptionMapper extends DDHubHeaderMapper
    implements ExceptionMapper<InvalidPayloadException> {

  @Inject
  Logger logger;

  @Context
  HttpHeaders httpHeaders;

  @Override
  public Response toResponse(final InvalidPayloadException exception) {
    ErrorResponse error = new ErrorResponse("17", exception.getMessage());
    this.logger.error("[" + userDid() + "][" + requestId() + "]" + JsonbBuilder.create().toJson(error));
    return Response.status(400).entity(error).build();
  }

}
