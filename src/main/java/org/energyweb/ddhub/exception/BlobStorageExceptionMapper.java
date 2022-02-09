package org.energyweb.ddhub.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.energyweb.ddhub.helper.ErrorResponse;

import com.azure.storage.blob.models.BlobStorageException;

@Provider
public class BlobStorageExceptionMapper implements ExceptionMapper<BlobStorageException> {
	@Override
	public Response toResponse(final BlobStorageException exception) {
		return Response.status(400).entity(new ErrorResponse("40", exception.getMessage())).build();
	}
}
