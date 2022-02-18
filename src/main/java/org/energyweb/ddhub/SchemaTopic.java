package org.energyweb.ddhub;

import java.util.Arrays;

import javax.enterprise.context.RequestScoped;
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
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTOCreate;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;

import io.quarkus.security.Authenticated;

@Path("/topic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class SchemaTopic {

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
    
    @Inject
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "verifiedRoles")
    String roles;

    @POST
    @RequestBodySchema(TopicDTOCreate.class)
    @Parameter(description = "test",example = "dev.test.a")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTO.class)))
    @Authenticated
    public Response createSchema(@NotNull @Valid TopicDTO topic) {
    	topic.setOwner(DID);
        topicRepository.save(topic);
        return Response.ok().entity(topic).build();
    }

    @GET
    @Path("createindex")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Tags(value = @Tag(name = "Internal", description = "All the methods"))
    @Authenticated
    public Response createSchemaIndex() {
        Document index = new Document("namespace", 1);
        index.append("owner", 1);
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
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = TopicDTO.class)))
    @Authenticated
    public Response listOfVersionById(@NotNull @PathParam("id") String id) {
    	topicRepository.validateTopicIds(Arrays.asList(id));
        return Response.ok().entity(topicVersionRepository.findListById(id,DID)).build();
    }

    @GET
    @Path("{id}/version/{versionNumber}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTO.class)))
    @Authenticated
    public Response topicVersionByNumber(@NotNull @PathParam("id") String id,
            @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions") @NotNull @PathParam("versionNumber") String versionNumber) {
    	topicRepository.validateTopicIds(Arrays.asList(id));
    	return Response.ok().entity(topicVersionRepository.findByIdAndVersion(id, versionNumber,DID)).build();
    }

    @GET
    @Path("list")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = TopicDTO.class)))
    @Authenticated
    public Response listOfSchema() {
        return Response.ok().entity(topicRepository.listAllBy(DID)).build();
    }

    @PATCH
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response updateSchema(@NotNull @Valid TopicDTO topic) {
    	topicRepository.validateTopicIds(Arrays.asList(topic.getId()));
        topicRepository.updateTopic(topic);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    @DELETE
    @Path("{id}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response deleteSchema(@NotNull @PathParam("id") String id) {
    	topicRepository.validateTopicIds(Arrays.asList(id));
        topicRepository.deleteTopic(id);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

}