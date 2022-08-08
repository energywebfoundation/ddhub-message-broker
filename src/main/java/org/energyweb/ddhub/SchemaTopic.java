package org.energyweb.ddhub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.bind.JsonbBuilder;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.bson.Document;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.dto.TopicDTOCreate;
import org.energyweb.ddhub.dto.TopicDTOGetPage;
import org.energyweb.ddhub.dto.TopicDTOPage;
import org.energyweb.ddhub.dto.TopicDTOSchema;
import org.energyweb.ddhub.dto.TopicDTOUpdate;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;
import org.jboss.logging.Logger;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;

import io.quarkus.security.Authenticated;

@Path("/topics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class SchemaTopic {

    @Inject
    Logger logger;

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
    @Claim(value = "roles")
    String roles;

    @Counted(name = "topics_post_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "topics_post_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @RequestBodySchema(TopicDTOCreate.class)
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTO.class)))
    @Authenticated
    public Response createSchema(@NotNull @Valid TopicDTO topic) {
        topic.validateOwner(roles);
        if (!topic.validOwner()) {
            ErrorResponse error = new ErrorResponse("12", "Owner : " + topic.getOwner() + " validation failed");
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(error));
            return Response.status(400).entity(error).build();
        }

        if (!topic.validateSchemaType()) {
            ErrorResponse error = new ErrorResponse("13", "schema validation failed");
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(error));
            return Response.status(400).entity(error).build();
        }

        topic.setDid(DID);
        topicRepository.save(topic);
        return Response.ok().entity(topic).build();
    }

    @Counted(name = "createindex_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "createindex_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @GET
    @Path("createindex")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Tags(value = @Tag(name = "Internal", description = "All the methods"))
    @Authenticated
    public Response createSchemaIndex() {
        Document index = new Document("name", 1);
        index.append("owner", 2);
        index.append("deleted", 3);
        index.append("deletedDate", 4);
        mongoClient.getDatabase("ddhub").getCollection("schema").dropIndexes();
        mongoClient.getDatabase("ddhub").getCollection("schema").createIndex(index, new IndexOptions().unique(true));

        Document version = new Document("topicId", 1);
        version.append("version", 2);
        version.append("deleted", 3);
        version.append("deletedDate", 4);
        mongoClient.getDatabase("ddhub").getCollection("schema_version").dropIndexes();
        mongoClient.getDatabase("ddhub").getCollection("schema_version").createIndex(version,new IndexOptions().unique(true));

        Document fqcn = new Document("fqcn", 1);
        mongoClient.getDatabase("ddhub").getCollection("channel").dropIndexes();
        mongoClient.getDatabase("ddhub").getCollection("channel").createIndex(fqcn, new IndexOptions().unique(true));

        Document did = new Document("did", 1);
        mongoClient.getDatabase("ddhub").getCollection("role_owner").dropIndexes();
        mongoClient.getDatabase("ddhub").getCollection("role_owner").createIndex(did, new IndexOptions().unique(true));

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    @Counted(name = "topics_get_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "topics_get_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @GET
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTOGetPage.class)))
    @Authenticated
    public Response queryByOwnerNameTags(@NotNull @NotEmpty @QueryParam("owner") String owner,
            @QueryParam("name") String name, @DefaultValue("1") @QueryParam("page") int page,
            @DefaultValue("0") @QueryParam("limit") int size,@DefaultValue("false") @QueryParam("includeDeleted") boolean includeDeleted, @QueryParam("tags") String... tags)
            throws ValidationException {
        if (page > 1 && size == 0)
            return Response.status(400).entity(new ErrorResponse("14", "Required to set limit with page > 1")).build();
        
        
        return Response.ok().entity(topicRepository.queryByOwnerNameTags(owner, name, page, size, includeDeleted, tags)).build();
    }

    @Counted(name = "search_get_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "search_get_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @GET
    @Path("search")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTOPage.class)))
    @Authenticated
    public Response queryByOwnerOrName(@NotNull @NotEmpty @QueryParam("keyword") String keyword, @QueryParam("owner") String owner,
            @DefaultValue("1") @QueryParam("page") int page, @DefaultValue("0") @QueryParam("limit") int size)
            throws ValidationException {
        return Response.ok().entity(topicRepository.queryByOwnerOrName(keyword, owner, page, size)).build();
    }

    @Counted(name = "count_get_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "count_get_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @GET
    @Path("count")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = HashMap.class)))
    @Authenticated
    public Response countByOwnerNameTags(@NotNull @NotEmpty @QueryParam("owner") String... owner)
            throws ValidationException {
    	JsonArrayBuilder response = Json.createArrayBuilder();
    	topicRepository.countByOwner(owner).forEach((_owner,count)->{
    		response.add(Json.createObjectBuilder()
    				.add("owner", _owner)
    				.add("count", count));
    	});
        return Response.ok().entity(response.build()).build();
    }

    @Counted(name = "id-versions_get_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "id-versions_get_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @GET
    @Path("{id}/versions")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTOPage.class)))
    @Authenticated
    public Response listOfVersionById(@NotNull @PathParam("id") String id,
            @DefaultValue("1") @QueryParam("page") int page, @DefaultValue("0") @QueryParam("limit") int size, @DefaultValue("false") @QueryParam("includeDeleted") boolean includeDeleted) {
        if (page > 1 && size == 0) {
            return Response.status(400).entity(new ErrorResponse("14", "Required to set limit with page > 1")).build();
        }
        topicRepository.validateTopicIds(Arrays.asList(id),includeDeleted);
        return Response.ok().entity(topicVersionRepository.findListById(id, page, size, includeDeleted)).build();
    }

    @Counted(name = "id-versions-number_get_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "id-versions-number_get_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @GET
    @Path("{id}/versions/{versionNumber}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTO.class)))
    @Authenticated
    public Response topicVersionByNumber(@NotNull @PathParam("id") String id,
            @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions") @NotNull @PathParam("versionNumber") String versionNumber) {
        topicRepository.validateTopicIds(Arrays.asList(id));
        return Response.ok().entity(topicVersionRepository.findByIdAndVersion(id, versionNumber)).build();
    }

    @Counted(name = "id-versions-number_put_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "id-versions-number_put_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @PUT
    @Path("{id}/versions/{versionNumber}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTO.class)))
    @Authenticated
    public Response updateTopicVersionByNumber(@NotNull @Valid TopicDTOSchema _topic,
            @NotNull @PathParam("id") String id,
            @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions") @NotNull @PathParam("versionNumber") String versionNumber) {
        topicRepository.validateTopicIds(Arrays.asList(id));
        // topicVersionRepository.findByIdAndVersion(id,versionNumber);
        TopicDTO topic = topicRepository.findTopicBy(id, versionNumber);
        topic.setSchema(_topic.getSchema());
        topic.validateOwner(roles);
        if (!topic.validOwner()) {
            ErrorResponse error = new ErrorResponse("12", "Owner : " + topic.getOwner() + " validation failed");
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(error));
            return Response.status(400).entity(error).build();
        }

        if (!topic.validateSchemaType()) {
            ErrorResponse error = new ErrorResponse("13", "schema validation failed");
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(error));
            return Response.status(400).entity(error).build();
        }
        return Response.ok().entity(topicRepository.updateByIdAndVersion(id, versionNumber, _topic.getSchema(), DID))
                .build();
    }

    @Counted(name = "id_put_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "id_put_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @PUT
    @Path("{id}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = TopicDTO.class)))
    @Authenticated
    public Response updateSchema(@NotNull @Valid TopicDTOUpdate _topic, @NotNull @PathParam("id") String id) {
        topicRepository.validateTopicIds(Arrays.asList(id));
        TopicDTO topic = topicRepository.findTopicBy(id, null);
        topic.validateOwner(roles);
        if (!topic.validOwner()) {
            ErrorResponse error = new ErrorResponse("12", "Owner : " + topic.getOwner() + " validation failed");
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(error));
            return Response.status(400).entity(error).build();
        }

        if (Optional.ofNullable(_topic.getTags()).isPresent()) {
            topic.setTags(_topic.getTags());

            topic.setDid(DID);
            topicRepository.updateTopic(topic);
        }

        return Response.ok().entity(topic).build();
    }

    @Counted(name = "id_del_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "id_del_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @DELETE
    @Path("{id}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response deleteSchema(@NotNull @PathParam("id") String id) {
        topicRepository.validateTopicIds(Arrays.asList(id));
        topicRepository.deleteTopic(id);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

    @Counted(name = "id-versions-number_del_count", description = "", tags = {"ddhub=topics"}, absolute = true)
    @Timed(name = "id-versions-number_del_timed", description = "", tags = {"ddhub=topics"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @DELETE
    @Path("{id}/versions/{versionNumber}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response deleteSchemaVersion(@NotNull @PathParam("id") String id,
            @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions") @NotNull @PathParam("versionNumber") String version) {
        topicRepository.validateTopicIds(Arrays.asList(id));
        TopicDTO topic = topicVersionRepository.findByIdAndVersion(id, version);
        topic.validateOwner(roles);
        if (!topic.validOwner()) {
            ErrorResponse error = new ErrorResponse("12", "Owner : " + topic.getOwner() + " validation failed");
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(error));
            return Response.status(400).entity(error).build();
        }
        topicRepository.deleteTopic(id, version);
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

}