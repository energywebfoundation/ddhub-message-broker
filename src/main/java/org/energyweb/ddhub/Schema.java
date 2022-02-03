package org.energyweb.ddhub;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.bson.Document;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.repository.TopicRepository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;

@Path("/schema")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Schema {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    TopicRepository topicRepository;
    
    @Inject 
    MongoClient mongoClient;

    @POST
    public Response createSchema(Topic topic) {
        topicRepository.persist(topic);
        return Response.ok().entity(topic).build();
    }
    
    @GET
    @Path("createindex")
    public Response createSchemaIndex() {
    	Document index = new Document("namespace", 1);
    	mongoClient.getDatabase("ddhub").getCollection("schema").createIndex(index, new IndexOptions().unique(true));
        return Response.ok().entity("Success").build();
    }
    
    @GET
    @Path("list")
    public Response listOfSchema() {
        return Response.ok().entity(topicRepository.listAll()).build();
    }
    
    @PATCH
    public Response updateSchema(Topic topic) {
    	topicRepository.update(topic);
        return Response.ok().entity("Success").build();
    }

    @DELETE
    public Response deleteSchema(Topic topic) {
    	topicRepository.delete(topic);
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