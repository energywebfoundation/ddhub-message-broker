package org.energyweb.ddhub;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

@Path("/schema")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Schema {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @POST
    public Response createSchema() {
        return Response.ok().entity("Success").build();
    }
    
    @PATCH
    public Response updateSchema() {
        return Response.ok().entity("Success").build();
    }

    @DELETE
    public Response deleteSchema() {
    	return Response.ok().entity("Success").build();
    }

    @POST
    @Path("assignee")
    public Response assignee() {
    	return Response.ok().entity("Success").build();
    }
    
    @PATCH
    @Path("assignee")
    public Response updateAssignee() {
    	return Response.ok().entity("Success").build();
    }
    
    @DELETE 
    @Path("assignee")
    public Response deleteAssignee() {
    	return Response.ok().entity("Success").build();
    }

}