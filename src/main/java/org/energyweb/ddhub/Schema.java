package org.energyweb.ddhub;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.helper.DDHubResponse;
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
    public Response createSchema(@NotNull @Valid TopicDTO topic) {
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
        mongoClient.getDatabase("ddhub").getCollection("schema_version").createIndex(version,
                new IndexOptions().unique(true));

        Document fqcn = new Document("fqcn", 1);
        mongoClient.getDatabase("ddhub").getCollection("channel").createIndex(fqcn, new IndexOptions().unique(true));

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    @GET
    @Path("{id}/version")
    public Response listOfVersionById(@NotNull @PathParam("id") String id) {
        return Response.ok().entity(topicVersionRepository.findListById(id)).build();
    }

    @GET
    @Path("{id}/version/{versionNumber}")
    public Response topicVersionByNumber(@NotNull @PathParam("id") String id,
            @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions") @NotNull @PathParam("versionNumber") String versionNumber) {
        return Response.ok().entity(topicVersionRepository.findByIdAndVersion(id, versionNumber)).build();
    }

    @GET
    @Path("list")
    public Response listOfSchema() {
        return Response.ok().entity(topicRepository.listAll()).build();
    }

    @PATCH
    public Response updateSchema(@NotNull @Valid TopicDTO topic) {
        topicRepository.updateTopic(topic);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteSchema(@NotNull @PathParam("id") String id) {
        topicRepository.deleteTopic(id);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

}