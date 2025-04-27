package org.energyweb.ddhub;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import org.energyweb.ddhub.dto.ExtChannelDTO;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.helper.ResponseWithStatus;
import org.energyweb.ddhub.helper.ReturnAnonymousKeyMessage;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.RoleOwnerRepository;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import com.mongodb.MongoException;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerConfiguration.Builder;
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

    @ConfigProperty(name = "NATS_REPLICAS_SIZE")
    OptionalInt natsReplicasSize;

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
    public Response initExtChannel(@Valid ExtChannelDTO extChannelDTO)
            throws IOException, JetStreamApiException, InterruptedException, ParseException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        channelDTO.setMaxMsgAge(natsMaxAge);
        channelDTO.setMaxMsgSize(natsMaxSize);
        Connection nc = Nats.connect(natsConnectionOption());
        JetStreamManagement jsm = nc.jetStreamManagement();

        List<ReturnAnonymousKeyMessage> status = new ArrayList<ReturnAnonymousKeyMessage>();

        try {
            channelRepository.findByFqcn(DID);
            Set<String> streamsAnonymous = new HashSet<>();
            if (extChannelDTO != null) {
                extChannelDTO.getAnonymousKeys().stream().filter(e -> streamsAnonymous.add(e.getAnonymousKey()))
                        .collect(Collectors.toList()).forEach(key -> {
                            try {
                                ChannelDTO channelAnonymousKey = new ChannelDTO();
                                channelAnonymousKey.setMaxMsgAge(natsMaxAge);
                                channelAnonymousKey.setMaxMsgSize(natsMaxSize);
                                channelAnonymousKey.setFqcn(key.getAnonymousKey());
                                StreamConfiguration streamConfig = StreamConfiguration.builder()
                                        .name(channelAnonymousKey.streamName())
                                        .description(DID)
                                        .addSubjects(channelAnonymousKey.subjectNameAll())
                                        .maxAge(Duration.ofMillis(channelAnonymousKey.getMaxMsgAge()))
                                        .maxMsgSize(channelAnonymousKey.getMaxMsgSize())
                                        .replicas(natsReplicasSize.orElse(ChannelDTO.DEFAULT_REPLICAS_SIZE))
                                        .duplicateWindow(
                                                Duration.ofSeconds(
                                                        duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW))
                                                        .toMillis())
                                        .build();
                                jsm.addStream(streamConfig);
                                channelAnonymousKey.setOwnerdid(DID);
                                channelRepository.save(channelAnonymousKey);
                                status.add(new ReturnAnonymousKeyMessage(key.getAnonymousKey(), "Success", ""));
                                try {
                                    ChannelDTO channelAnonymousKeyKey = new ChannelDTO();
                                    channelAnonymousKeyKey.setFqcn(key.getAnonymousKey() + ".keys");
                                    jsm.addStream(StreamConfiguration.builder()
                                            .name(channelAnonymousKeyKey.streamName())
                                            .addSubjects(channelAnonymousKeyKey.subjectNameAll())
                                            .maxAge(Duration.ofMillis(channelAnonymousKey.getMaxMsgAge()))
                                            .maxMsgSize(channelAnonymousKey.getMaxMsgSize())
                                            .replicas(natsReplicasSize.orElse(ChannelDTO.DEFAULT_REPLICAS_SIZE))
                                            .duplicateWindow(
                                                    Duration.ofSeconds(
                                                            duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW))
                                                            .toMillis())
                                            .build());
                                } catch (IOException | JetStreamApiException e) {
                                    logger.info("[" + requestId + "]" + e.getMessage());
                                }
                            } catch (MongoException e) {
                                logger.warn("[" + requestId + "]" + e.getMessage());
                                if (e.getMessage().contains("E11000")) {
                                    status.add(new ReturnAnonymousKeyMessage(key.getAnonymousKey(), "Fail",
                                            "Record exists"));
                                } else {
                                    status.add(new ReturnAnonymousKeyMessage(key.getAnonymousKey(), "Fail",
                                            e.getMessage()));
                                }
                            } catch (IOException | JetStreamApiException e) {
                                logger.info("[" + requestId + "]" + e.getMessage());
                                status.add(
                                        new ReturnAnonymousKeyMessage(key.getAnonymousKey(), "Fail", e.getMessage()));
                            }
                        });
            }
        } catch (MongoException ex) {
            logger.info("[" + requestId + "] Channel not exist. creating channel:" + DID);
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name(channelDTO.streamName())
                    .addSubjects(channelDTO.subjectNameAll())
                    .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                    .maxMsgSize(channelDTO.getMaxMsgSize())
                    .replicas(natsReplicasSize.orElse(ChannelDTO.DEFAULT_REPLICAS_SIZE))
                    .duplicateWindow(
                            Duration.ofSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW)).toMillis())
                    .build();
            StreamInfo streamInfo = jsm.addStream(streamConfig);

            try {
                logger.info("[" + requestId + "] Channel not exist. creating channel keys:" + DID);
                ChannelDTO channelKey = new ChannelDTO();
                channelKey.setFqcn(DID + ".keys");
                jsm.addStream(StreamConfiguration.builder()
                        .name(channelKey.streamName())
                        .addSubjects(channelKey.subjectNameAll())
                        .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                        .maxMsgSize(channelDTO.getMaxMsgSize())
                        .replicas(natsReplicasSize.orElse(ChannelDTO.DEFAULT_REPLICAS_SIZE))
                        .duplicateWindow(
                                Duration.ofSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW))
                                        .toMillis())
                        .build());
            } catch (IOException | JetStreamApiException e) {
                logger.info("[" + requestId + "]" + e.getMessage());
            }

            channelDTO.setOwnerdid(DID);
            channelRepository.save(channelDTO);
        } finally {
            if (nc != null) {
                nc.close();
            }
        }
        ownerRepository.save(DID, verifiedRoles);

        return Response.ok().entity((!status.isEmpty()) ? new ResponseWithStatus("00", "Success", status)
                : new DDHubResponse("00", "Success")).build();

    }

    @POST
    @Counted(name = "clientdIdsUpdateWait_get_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "clientdIdsUpdateWait_get_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("clientdIdsUpdateWait")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)))
    @Authenticated
    public Response clientdIdsUpdateWait() throws IOException, InterruptedException {
        Connection nc = Nats.connect(natsConnectionOption());
        Map<String, List> result = new HashMap<String, List>();
        JetStreamManagement jsm = nc.jetStreamManagement();
        channelRepository.findAll().list().forEach(entity -> {
            List<String> _result = new ArrayList<String>();
            ChannelDTO channelDTO = new ChannelDTO();
            channelDTO.setFqcn(entity.getFqcn());
            try {
                jsm.getConsumers(channelDTO.streamName()).forEach(consumer -> {
                    try {
                        Builder builder = ConsumerConfiguration.builder(consumer.getConsumerConfiguration());
                        builder.ackWait((Duration.ofSeconds(1).toMillis()));
                        jsm.addOrUpdateConsumer(channelDTO.streamName(), builder.build());
                        _result.add(consumer.getName());
                    } catch (IOException | JetStreamApiException e) {
                        this.logger.info(e.getMessage());
                    }
                });
            } catch (IOException | JetStreamApiException e) {
                this.logger.info(e.getMessage());
            }
            result.put(channelDTO.streamName(), _result);

        });
        nc.close();

        return Response.ok().entity(result).build();

    }

    @GET
    @Counted(name = "clientdIds_get_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "clientdIds_get_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("clientIds")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)))
    @Authenticated
    public Response clientdIds() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        Connection nc = Nats.connect(natsConnectionOption());
        JetStreamManagement jsm = nc.jetStreamManagement();
        Set<String> result = new HashSet<String>();
        jsm.getConsumerNames(channelDTO.streamName()).forEach(id -> {
            if (id.contains(":#:")) {
                result.add(id.split(":#:")[0]);
            } else {
                result.add(id);
            }
        });
        nc.close();

        return Response.ok().entity(result).build();

    }

    @DELETE
    @Counted(name = "clientdIds_delete_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "clientdIds_delete_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("clientIds")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)))
    @Authenticated
    public Response removeClientdIds(@NotNull @Valid ClientDTO clientDTO) throws IOException, InterruptedException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        Connection nc = Nats.connect(natsConnectionOption());
        JetStreamManagement jsm = nc.jetStreamManagement();
        Set<String> result = new HashSet<String>();
        clientDTO.getClientIds().forEach(id -> {
            try {
                jsm.getConsumerNames(channelDTO.streamName()).stream().filter(consumer -> consumer.contains(id))
                        .forEach(consumer -> {
                            try {
                                if (jsm.deleteConsumer(channelDTO.streamName(), consumer)) {
                                    result.add(id);
                                }
                            } catch (IOException | JetStreamApiException e) {
                            }
                        });
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
        try {
            Connection nc = Nats.connect(natsConnectionOption());
            JetStreamManagement jsm = nc.jetStreamManagement();
            channelRepository.findAll().list().forEach(entity -> {
                ChannelDTO channelDTO = new ChannelDTO();
                channelDTO.setFqcn(entity.getFqcn());
                channelDTO.setMaxMsgAge(natsMaxAge);
                channelDTO.setMaxMsgSize(natsMaxSize);
                logger.info("[" + requestId + "] re-creating channel:" + entity.getFqcn());
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(channelDTO.streamName())
                        .addSubjects(channelDTO.subjectNameAll())
                        .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                        .replicas(natsReplicasSize.orElse(ChannelDTO.DEFAULT_REPLICAS_SIZE))
                        .maxMsgSize(channelDTO.getMaxMsgSize())
                        .duplicateWindow(Duration.ofSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW))
                                .toMillis())
                        .build();
                try {
                    StreamInfo streamInfo = jsm.addStream(streamConfig);
                } catch (IOException | JetStreamApiException e) {
                    logger.info("[" + requestId + "]" + e.getMessage());
                }
            });
            nc.close();
        } catch (IOException | InterruptedException e) {
            logger.info("[" + requestId + "]" + e.getMessage());
        }

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();

    }

    @POST
    @Counted(name = "reinitExtChannelKeys_post_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "reinitExtChannelKeys_post_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Tags(value = @Tag(name = "Internal", description = "All the methods"))
    @Path("reinitExtChannelKeys")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response reInitExtChannelKeys()
            throws IOException, JetStreamApiException, InterruptedException, ParseException {
        try {
            Connection nc = Nats.connect(natsConnectionOption());
            JetStreamManagement jsm = nc.jetStreamManagement();
            channelRepository.findAll().list().forEach(entity -> {
                ChannelDTO channelDTO = new ChannelDTO();
                channelDTO.setFqcn(entity.getFqcn() + ".keys");
                channelDTO.setMaxMsgAge(natsMaxAge);
                channelDTO.setMaxMsgSize(natsMaxSize);
                logger.info("[" + requestId + "] re-creating channel keys: " + entity.getFqcn());
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(channelDTO.streamName())
                        .addSubjects(channelDTO.subjectNameAll())
                        .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                        .replicas(natsReplicasSize.orElse(ChannelDTO.DEFAULT_REPLICAS_SIZE))
                        .maxMsgSize(channelDTO.getMaxMsgSize())
                        .duplicateWindow(Duration.ofSeconds(duplicateWindow.orElse(ChannelDTO.DEFAULT_DUPLICATE_WINDOW))
                                .toMillis())
                        .build();
                try {
                    StreamInfo streamInfo = jsm.addStream(streamConfig);
                } catch (IOException | JetStreamApiException e) {
                    logger.info("[" + requestId + "]" + e.getMessage());
                }
            });
            nc.close();
        } catch (IOException | InterruptedException e) {
            logger.info("[" + requestId + "]" + e.getMessage());
        }

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();

    }

    @DELETE
    @Counted(name = "removeChannel_delete_count", description = "", tags = { "ddhub=channel" }, absolute = true)
    @Timed(name = "removeChannel_delete_timed", description = "", tags = {
            "ddhub=channel" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("stream/{name}")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)))
    @Authenticated
    public Response removeChannel(
            @NotNull @PathParam("name") @Pattern(regexp = "^[^&<>\"'/\\\\\\-\\.\\r\\n\\t]*$", message = "Invalid characters detected.") String streamName)
            throws IOException, InterruptedException, JetStreamApiException {
        ChannelDTO channelDTO = new ChannelDTO();

        if (channelDTO.validateAnonymousKey(streamName) != null) {
            return Response.status(400).entity(channelDTO.validateAnonymousKey(streamName)).build();
        }

        channelDTO.setFqcn(streamName.trim());
        Connection nc = Nats.connect(natsConnectionOption());
        JetStreamManagement jsm = nc.jetStreamManagement();
        jsm.deleteStream(channelDTO.streamName());
        channelRepository.deleteByFqcn(streamName);
        try {
            jsm.deleteStream("keys_" + channelDTO.streamName());
        } catch (IOException | JetStreamApiException e) {
            logger.warn("[" + requestId + "]" + e.getMessage());
        }
        nc.close();

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();

    }

    private Options natsConnectionOption() {
        return new Options.Builder().server(natsJetstreamUrl).maxReconnects(ChannelDTO.MAX_RECONNECTS)
                .connectionTimeout(Duration.ofSeconds(ChannelDTO.TIMEOUT)). // Set timeout
                build();
    }
}