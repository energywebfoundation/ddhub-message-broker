package org.energyweb.ddhub;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.dto.ChannelDTOCreate;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.TopicRepository;
import org.jboss.logging.Logger;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamConfiguration.Builder;
import io.nats.client.api.StreamInfo;
import io.quarkus.security.Authenticated;

@Path("/channel")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(value = @Tag(name = "Channel", description = "All the methods"))
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "AuthServer", type = SecuritySchemeType.HTTP, scheme = "Bearer") })
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class Channel {
	
	@Inject
	Logger logger;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "NATS_JS_URL")
    String natsJetstreamUrl;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    TopicRepository topicRepository;

    @Inject
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "verifiedRoles")
    String roles;

    @PATCH
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = ChannelDTO.class)))
    @Authenticated
    public Response updateChannel(@Valid @NotNull ChannelDTO channelDTO)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        topicRepository.validateTopicIds(List.copyOf(Optional.ofNullable(channelDTO.getTopicIds()).orElse(new HashSet<String>())));
        channelRepository.validateChannel(channelDTO.getFqcn());

        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        StreamInfo _streamInfo = jsm.getStreamInfo(channelDTO.streamName());
        StreamConfiguration streamConfig = StreamConfiguration.builder(_streamInfo.getConfiguration())
                .addSubjects(channelDTO.findArraySubjectName())
                .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                .maxMsgSize(channelDTO.getMaxMsgSize())
                .duplicateWindow(0)
                .build();

        StreamInfo streamInfo = jsm.updateStream(streamConfig);
        channelDTO.setUpdateBy(DID);
        channelRepository.updateChannel(channelDTO);
        nc.close();
        return Response.ok().entity(channelDTO).build();
    }

    @POST
    @RequestBodySchema(ChannelDTOCreate.class)
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = ChannelDTO.class)))
    @Authenticated
    public Response createChannel(@Valid @NotNull ChannelDTO channelDTO)
            throws IOException, InterruptedException, ExecutionException, TimeoutException, JetStreamApiException {
        topicRepository.validateTopicIds(List.copyOf(Optional.ofNullable(channelDTO.getTopicIds()).orElse(new HashSet<String>())));
        // channelRepository.validateChannel(channelDTO.getFqcn());

        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(channelDTO.streamName())
                .addSubjects(channelDTO.findArraySubjectName())
                .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                .maxMsgSize(channelDTO.getMaxMsgSize())
                .duplicateWindow(0)
                .build();
        StreamInfo streamInfo = jsm.addStream(streamConfig);
        channelDTO.setOwner(DID);
        channelRepository.save(channelDTO);
        nc.close();
        return Response.ok().entity(channelDTO).build();
    }

    @GET
    @Path("pubsub")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = ChannelDTO.class)))
    @Authenticated
    public Response listOfChannel() {
        return Response.ok().entity(channelRepository.listChannel(DID)).build();

    }
    
    @PUT
    @Path("{fqcn}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = ChannelDTO.class)))
    @Authenticated
    public Response channelAddTopicByfqcn(@PathParam("fqcn") String fqcn, @NotNull @QueryParam("topicIds") String... topicIds)
            throws IOException, InterruptedException, JetStreamApiException {
    	 topicRepository.validateTopicIds(Arrays.asList(topicIds));
         channelRepository.validateChannel(fqcn);

         ChannelDTO channelDTO = channelRepository.findByFqcn(fqcn);
         if(channelDTO.getTopicIds() == null) {
        	 channelDTO.setTopicIds(Set.copyOf(Arrays.asList(topicIds)));
         }else {
        	 channelDTO.getTopicIds().addAll(Arrays.asList(topicIds));
         }
         
         Connection nc = Nats.connect(natsJetstreamUrl);
         JetStreamManagement jsm = nc.jetStreamManagement();
         StreamInfo _streamInfo = jsm.getStreamInfo(channelDTO.streamName());
         StreamConfiguration streamConfig = StreamConfiguration.builder(_streamInfo.getConfiguration())
                 .addSubjects(channelDTO.findArraySubjectName())
                 .build();

         StreamInfo streamInfo = jsm.updateStream(streamConfig);
         logger.info(streamInfo);
         channelDTO.setUpdateBy(DID);
         channelRepository.updateChannel(channelDTO);
         nc.close();
         return Response.ok().entity(channelDTO).build();
    }

    @GET
    @Path("{fqcn}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = ChannelDTO.class)))
    @Authenticated
    public Response channelByfqcn(@PathParam("fqcn") String fqcn)
            throws IOException, InterruptedException, JetStreamApiException {
        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(fqcn);
        StreamInfo _streamInfo = jsm.getStreamInfo(channelDTO.streamName());

        channelDTO = channelRepository.findByFqcn(fqcn);

        nc.close();
        return Response.ok().entity(channelDTO).build();
    }

    @DELETE
    @Path("{fqcn}")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response deletefqcn(@PathParam("fqcn") String fqcn)
            throws IOException, JetStreamApiException, InterruptedException {
        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(fqcn);
        jsm.deleteStream(channelDTO.streamName());

        channelRepository.deleteByFqcn(fqcn);

        nc.close();
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
    }

}