package org.energyweb.ddhub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.FileUploadDTO;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.FileUploadRepository;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.PublishOptions;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.PublishAck;
import io.quarkus.security.Authenticated;

@Path("/message")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class Message {
    @Inject
    Logger log;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "NATS_JS_URL")
    String natsJetstreamUrl;

    @Inject
    Validator validator;

    @Inject
    TopicRepository topicRepository;
    
    @Inject
	TopicVersionRepository topicVersionRepository;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    FileUploadRepository fileUploadRepository;
    
    @Inject
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "verifiedRoles")
    String roles;
    

    @POST
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response publish(@Valid @NotNull MessageDTO messageDTO)
            throws InterruptedException, JetStreamApiException, TimeoutException, IOException {
        topicRepository.validateTopicIds(Arrays.asList(messageDTO.getTopicId()));
        topicVersionRepository.validateByIdAndVersion(messageDTO.getTopicId(), messageDTO.getTopicVersion());
        channelRepository.validateChannel(messageDTO.getFqcn(),messageDTO.getTopicId(),DID);

        Connection nc = Nats.connect(natsJetstreamUrl);
        
        JetStream js = nc.jetStream();
        PublishOptions.Builder pubOptsBuilder = PublishOptions.builder()
        		.messageId(messageDTO.getTransactionId());
        
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("payload",  messageDTO.getPayload());
        builder.add("topicVersion",  messageDTO.getTopicVersion());
        builder.add("owner",  DID);
        builder.add("signature",  messageDTO.getSignature());
        
        PublishAck pa = js.publish(messageDTO.subjectName(),
        		builder.build().toString().getBytes(StandardCharsets.UTF_8),
        		(messageDTO.getTransactionId() != null) ? pubOptsBuilder.build() : null);
        
        nc.flush(Duration.ZERO);
        nc.close();
        return Response.ok().entity(new DDHubResponse("00", "Success")).build();
      
    }

    @GET
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = MessageDTO.class)))
    @Authenticated
    public Response pull(@NotNull @QueryParam("fqcn") String fqcn,
            @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") @NotNull @QueryParam("topicId") String topicId,
            @DefaultValue("default") @QueryParam("clientId") String clientId,
            @DefaultValue("1") @QueryParam("amount") Integer amount)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        topicRepository.validateTopicIds(Arrays.asList(topicId));
        channelRepository.validateChannel(fqcn,topicId,DID);

        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStream js = nc.jetStream();
        ConsumerConfiguration cc = ConsumerConfiguration.builder()
                .ackWait(Duration.ofMillis(2500))
                .build();
        PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                .durable(clientId) // required
                .configuration(cc)
                .build();
        MessageDTO msg = new MessageDTO();
        msg.setFqcn(fqcn);
        msg.setTopicId(topicId);
        JetStreamSubscription sub = js.subscribe(msg.subjectName(), pullOptions);
        nc.flush(Duration.ofSeconds(1));

        List<io.nats.client.Message> messages = sub.fetch(amount, Duration.ofSeconds(3));
        messages.forEach(io.nats.client.Message::ack);
        List<MessageDTO> messageDTOs = new ArrayList<MessageDTO>();
        for (io.nats.client.Message m : messages) {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setPayload(new String(m.getData()));
            messageDTO.setFqcn(fqcn);
            messageDTO.setTopicId(topicId);
            messageDTOs.add(messageDTO);
        }
        nc.close();
        return Response.ok().entity(messageDTOs).build();
    }

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = DDHubResponse.class)))
    @Authenticated
    public Response uploadFile(@Valid @MultipartForm FileUploadDTO data, @HeaderParam("Authorization") String token) {
        topicRepository.validateTopicIds(Arrays.asList(data.getTopicId()));
        channelRepository.validateChannel(data.getFqcn(),data.getTopicId(),DID);;
        data.setOwner(DID);
        String fileId = fileUploadRepository.save(data, channelRepository.findByFqcn(data.getFqcn()));
        data.setFileName(fileId);
        return Response.ok().entity(producerTemplate.sendBodyAndProperty("direct:azureupload", ExchangePattern.InOut, data,"token",token))
                .build();
    }

    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Authenticated
    public Response downloadFile(@NotNull @QueryParam("fileId") String fileId) {
        MessageDTO messageDTO = fileUploadRepository.findByFileId(fileId);
        String filename = fileUploadRepository.findFilenameByFileId(fileId);

        return Response
                .ok(producerTemplate.sendBodyAndHeader("direct:azuredownload", ExchangePattern.InOut, null,
                        "CamelAzureStorageBlobBlobName", messageDTO.storageName() + fileId),
                        MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();

    }
}