package org.energyweb.ddhub;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.dto.ClientDTO;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.RoleOwnerRepository;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import com.mongodb.MongoException;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamOptions;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.StreamConfiguration;
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

    @ConfigProperty(name = "NATS_JS_URL")
    String natsJetstreamUrl;

    @ConfigProperty(name = "NATS_MAX_AGE")
    long natsMaxAge;

    @ConfigProperty(name = "NATS_MAX_SIZE")
    long natsMaxSize;

    @ConfigProperty(name = "NATS_MAX_CLIENT_ID")
    OptionalLong natsMaxClientId;
    
    @ConfigProperty(name = "DUPLICATE_WINDOW")
    OptionalLong duplicateWindow;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    RoleOwnerRepository ownerRepository;

    @Inject
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "roles")
    String verifiedRoles;
    
    @HeaderParam("X-Request-Id")
    String requestId;

    @POST
    @Counted(name = "initExtChannel_post_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "initExtChannel_post_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("initExtChannel")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response initExtChannel() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        channelDTO.setMaxMsgAge(natsMaxAge);
        channelDTO.setMaxMsgSize(natsMaxSize);
        try {
            channelRepository.findByFqcn(DID);
        } catch (MongoException ex) {
            logger.info("[" + requestId + "] Channel not exist. creating channel:" + DID);
            Connection nc = Nats.connect(natsConnectionOption());
            JetStreamManagement jsm = nc.jetStreamManagement();
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name(channelDTO.streamName())
                    .addSubjects(channelDTO.subjectNameAll())
                    .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                    .maxMsgSize(channelDTO.getMaxMsgSize())
                    .maxConsumers(natsMaxClientId.orElse(ChannelDTO.DEFAULT_CLIENT_ID_SIZE))
                    .duplicateWindow(
                            Duration.ofSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW)).toMillis())
                    .build();
            StreamInfo streamInfo = jsm.addStream(streamConfig);
            nc.close();
            channelDTO.setOwnerdid(DID);
            channelRepository.save(channelDTO);
        }
        ownerRepository.save(DID, verifiedRoles);

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();

    }
    
    @GET
    @Counted(name = "clientdIds_get_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "clientdIds_get_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("clientdIds")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)))
    @Authenticated
    public Response clientdIds() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        Connection nc = Nats.connect(natsConnectionOption());
        JetStreamManagement jsm = nc.jetStreamManagement();
        List<String> result = jsm.getConsumerNames(channelDTO.streamName());
        nc.close();

        return Response.ok().entity(result).build();

    }
    
    @DELETE
    @Counted(name = "clientdIds_delete_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "clientdIds_delete_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("clientdIds")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)))
    @Authenticated
    public Response removeClientdIds(@NotNull @Valid ClientDTO clientDTO) throws IOException, InterruptedException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        Connection nc = Nats.connect(natsConnectionOption());
        JetStreamManagement jsm = nc.jetStreamManagement();
        Set<String> result = new HashSet<String>();
        clientDTO.getClientIds().forEach(id ->{
        	try {
				if(jsm.deleteConsumer(channelDTO.streamName(),id)) {
					result.add(id);
				}
			} catch (IOException | JetStreamApiException e) {
			}
        });
        nc.close();

		return Response.ok().entity(result).build();

    }

    @POST
    @Counted(name = "reinitExtChannel_post_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "reinitExtChannel_post_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Tags(value = @Tag(name = "Internal", description = "All the methods"))
    @Path("reinitExtChannel")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response reInitExtChannel() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        channelRepository.findAll().list().forEach(entity -> {
            ChannelDTO channelDTO = new ChannelDTO();
            channelDTO.setFqcn(entity.getFqcn());
            channelDTO.setMaxMsgAge(natsMaxAge);
            channelDTO.setMaxMsgSize(natsMaxSize);
            logger.info("[" + requestId + "] re-creating channel:" + entity.getFqcn());
            try {
                Connection nc = Nats.connect(natsConnectionOption());
                JetStreamManagement jsm = nc.jetStreamManagement();
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(channelDTO.streamName())
                        .addSubjects(channelDTO.subjectNameAll())
                        .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                        .maxMsgSize(channelDTO.getMaxMsgSize())
                        .maxConsumers(natsMaxClientId.orElse(ChannelDTO.DEFAULT_CLIENT_ID_SIZE))
                        .duplicateWindow(Duration.ofSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW))
                                .toMillis())
                        .build();
                StreamInfo streamInfo = jsm.addStream(streamConfig);
                nc.close();
            } catch (IOException | InterruptedException | JetStreamApiException e) {
                logger.info("[" + requestId + "]" + e.getMessage());
            }
        });

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();

    }
    
	private Options natsConnectionOption() {
		return new Options.Builder().
		        server(natsJetstreamUrl).
		        maxReconnects(ChannelDTO.MAX_RECONNECTS).
		        connectionTimeout(Duration.ofSeconds(ChannelDTO.TIMEOUT)). // Set timeout
		        build();
	}
}