package org.energyweb.ddhub;

import java.io.IOException;
import java.time.Duration;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claim;
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
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.RoleOwnerRepository;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import com.mongodb.MongoException;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
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

    @ConfigProperty(name = "DUPLICATE_WINDOW")
    int duplicateWindow;

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

    @POST
    @Path("initExtChannel")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response initExtChannel() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(DID);
        channelDTO.setMaxMsgAge(86400000l);
        channelDTO.setMaxMsgSize(8388608l);
        Connection nc = Nats.connect(natsJetstreamUrl);
        try {
            JetStreamManagement jsm = nc.jetStreamManagement();
            StreamInfo _streamInfo = jsm.getStreamInfo(channelDTO.streamName());

            this.logger.info("[" + DID + "]" + JsonbBuilder.create().toJson(_streamInfo));
            channelRepository.validateChannel(DID);
        } catch (MongoException | JetStreamApiException ex) {
            logger.debug("Channel not exist. creating channel:" + DID);
            JetStreamManagement jsm = nc.jetStreamManagement();

            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name(channelDTO.streamName())
                    .addSubjects(channelDTO.subjectNameAll())
                    .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                    .maxMsgSize(channelDTO.getMaxMsgSize())
                    .duplicateWindow(duplicateWindow * 1000000000)
                    .build();
            StreamInfo streamInfo = jsm.addStream(streamConfig);
            channelDTO.setOwnerdid(DID);
            channelRepository.save(channelDTO);

            nc.close();
        }
        ownerRepository.save(DID, verifiedRoles);

        return Response.ok().entity(new DDHubResponse("00", "Success")).build();

    }
}