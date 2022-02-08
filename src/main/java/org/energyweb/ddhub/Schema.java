package org.energyweb.ddhub;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.bson.Document;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.model.Topic;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;

@Path("/topic")
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
    TopicVersionRepository topicVersionRepository;

    @Inject
    MongoClient mongoClient;

    @POST
    public Response createSchema(Topic topic) {
        topicRepository.save(topic);
        return Response.ok().entity(topic).build();
    }

    @GET
    @Path("createindex")
    public Response createSchemaIndex() {
        Document index = new Document("namespace", 1);
        mongoClient.getDatabase("ddhub").getCollection("schema").createIndex(index, new IndexOptions().unique(true));

        Document version = new Document("topicId", 1);
        version.append("version", 1);
        mongoClient.getDatabase("ddhub").getCollection("schema_version").createIndex(version, new IndexOptions().unique(true));

        Document fqcn = new Document("fqcn", 1);
        mongoClient.getDatabase("ddhub").getCollection("channel").createIndex(fqcn, new IndexOptions().unique(true));
        
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }
    
    @GET
    @Path("{id}/version")
    public Response listOfVersionById(@PathParam("id") String id) {
        return Response.ok().entity(topicVersionRepository.findListById(id)).build();
    }

    @GET
    @Path("{id}/version/{versionNumber}")
    public Response topicVersionByNumber(@PathParam("id") String id , @PathParam("versionNumber") Integer versionNumber) {
    	
        return Response.ok().entity(topicVersionRepository.findByIdAndVersion(id,versionNumber)).build();
    }
    
    @GET
    @Path("list")
    public Response listOfSchema() {
        return Response.ok().entity(topicRepository.listAll()).build();
    }

    @PATCH
    public Response updateSchema(Topic topic) {
        topicRepository.updateTopic(topic);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    @DELETE
    public Response deleteSchema(Topic topic) {
        topicRepository.delete(topic);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    

}