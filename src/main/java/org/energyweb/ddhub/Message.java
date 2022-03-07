package org.energyweb.ddhub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.energyweb.ddhub.dto.FileUploadDTO;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.SearchMessageDTO;
import org.energyweb.ddhub.helper.DDHubResponse;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.FileUploadRepository;
import org.energyweb.ddhub.repository.MessageRepository;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import com.google.gson.Gson;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.PublishOptions;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerConfiguration.Builder;
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
    MessageRepository messageRepository;

    @Inject
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "verifiedRoles")
    String roles;

    @POST
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = HashMap.class)))
    @Authenticated
    public Response publish(@Valid @NotNull MessageDTO messageDTO)
            throws InterruptedException, JetStreamApiException, TimeoutException, IOException {
        topicRepository.validateTopicIds(Arrays.asList(messageDTO.getTopicId()));
        topicVersionRepository.validateByIdAndVersion(messageDTO.getTopicId(), messageDTO.getTopicVersion());
        // channelRepository.validateChannel(messageDTO.getFqcn(),messageDTO.getTopicId(),DID);

        Connection nc = Nats.connect(natsJetstreamUrl);

        JetStream js = nc.jetStream();
        PublishOptions.Builder pubOptsBuilder = PublishOptions.builder()
                .messageId(messageDTO.getTransactionId());

        String id = messageRepository.save(messageDTO, DID);

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("id", id);
        builder.add("payload", messageDTO.getPayload());
        builder.add("topicVersion", messageDTO.getTopicVersion());
        builder.add("transactionId", Optional.ofNullable(messageDTO.getTransactionId()).orElse(""));
        builder.add("sender", DID);
        builder.add("signature", messageDTO.getSignature());
        builder.add("timestampNanos", String.valueOf( TimeUnit.NANOSECONDS.toNanos(new Date().getTime())));

        PublishAck pa = js.publish(messageDTO.subjectName(),
                builder.build().toString().getBytes(StandardCharsets.UTF_8),
                (messageDTO.getTransactionId() != null) ? pubOptsBuilder.build() : null);

        nc.flush(Duration.ZERO);
        nc.close();

        HashMap<String, String> map = new HashMap<>();
        map.put("id", id);
        return Response.ok().entity(map).build();

    }

    @POST
    @Path("search")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = MessageDTO.class)))
    @Authenticated
    public Response search(@Valid SearchMessageDTO messageDTO)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        // topicRepository.validateTopicIds(messageDTO.getTopicId());
        // channelRepository.validateChannel(messageDTO.getFqcn(),topicId,DID);
        messageDTO.setFqcn(DID);

        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStream js = nc.jetStream();

        Builder builder = ConsumerConfiguration.builder();
        builder.maxAckPending(Duration.ofSeconds(5).toMillis());
        builder.durable(messageDTO.getClientId()); // required
        Optional.ofNullable(messageDTO.getFrom()).ifPresent(lt->{
        	builder.durable(messageDTO.getClientId().concat(String.valueOf(lt.toEpochSecond(ZoneOffset.UTC))));
        	builder.startTime(lt.atZone(ZoneId.systemDefault()));
        });
        PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                .configuration(builder.build())
                .build();
        JetStreamSubscription sub = js.subscribe(messageDTO.subjectAll(), pullOptions);
        nc.flush(Duration.ofSeconds(1));

        List<MessageDTO> messageDTOs = new ArrayList<MessageDTO>();
        while (messageDTOs.size() < messageDTO.getAmount()) {
            List<io.nats.client.Message> messages = sub.fetch(messageDTO.getAmount(), Duration.ofSeconds(3));
            // messages.forEach(m -> m.inProgress());
            if (messages.isEmpty()) {
                break;
            }
            for (io.nats.client.Message m : messages) {
                m.inProgress();
                if (m.isStatusMessage()) {
                    m.nak();
                    continue;
                }
                if (messageDTO.getTopicId().stream().filter(id -> m.getSubject().contains(id)).findFirst().isEmpty()) {
                    continue;
                }

                HashMap<String, Object> natPayload = new Gson().fromJson(new String(m.getData()), HashMap.class);
                String sender = (String) natPayload.get("sender");
                if (messageDTO.getSenderId().stream().filter(id -> sender.contains(id)).findFirst().isEmpty()) {
                    continue;
                }

                MessageDTO message = new MessageDTO();
                message.setPayload((String) natPayload.get("payload"));
                message.setFqcn(messageDTO.getFqcn());
                message.setTopicId(m.getSubject().replaceFirst(DID.concat("."), ""));
                message.setId((String) natPayload.get("id"));
                message.setSenderDid(sender);
                message.setTopicVersion((String) natPayload.get("topicVersion"));
                message.setSignature((String) natPayload.get("signature"));
                message.setTimestampNanos(Long.valueOf((String) natPayload.get("timestampNanos")).longValue());
                messageDTOs.add(message);
                m.ack();
            }
        }
        sub.unsubscribe();
        nc.close();
        return Response.ok().entity(messageDTOs).build();
    }

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = HashMap.class)))
    @Authenticated
    public Response uploadFile(@Valid @MultipartForm FileUploadDTO data, @HeaderParam("Authorization") String token) {
        topicRepository.validateTopicIds(Arrays.asList(data.getTopicId()));
//        channelRepository.validateChannel(data.getFqcn(), data.getTopicId(), DID);
        data.setOwnerdid(DID);
        String fileId = fileUploadRepository.save(data, channelRepository.findByFqcn(data.getFqcn()));
        data.setFileName(fileId);
        return Response.ok()
                .entity(producerTemplate.sendBodyAndProperty("direct:azureupload", ExchangePattern.InOut, data, "token",token))
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