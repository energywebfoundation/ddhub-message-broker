package org.energyweb.ddhub;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
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
import javax.json.bind.JsonbBuilder;
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
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.energyweb.ddhub.dto.FileUploadChunkDTOs;
import org.energyweb.ddhub.dto.FileUploadDTOs;
import org.energyweb.ddhub.dto.InternalMessageDTO;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.MessageDTOs;
import org.energyweb.ddhub.dto.SearchInternalMessageDTO;
import org.energyweb.ddhub.dto.SearchMessageDTO;
import org.energyweb.ddhub.helper.MessageResponse;
import org.energyweb.ddhub.helper.Recipients;
import org.energyweb.ddhub.helper.ReturnErrorMessage;
import org.energyweb.ddhub.helper.ReturnMessage;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.FileUploadRepository;
import org.energyweb.ddhub.repository.MessageRepository;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.PublishOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerConfiguration.Builder;
import io.nats.client.api.PublishAck;
import io.quarkus.security.Authenticated;

@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class Message {
    @Inject
    Logger logger;

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
    @Claim(value = "roles")
    String roles;

    @ConfigProperty(name = "INTERNAL_TOPIC")
    String internalTopicId;

    @ConfigProperty(name = "INTERNAL_POSTFIX_CLIENT_ID", defaultValue = "20220510")
    String clientIdPostfix;

    @Counted(name = "messages_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "messages_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @Authenticated
    public Response publish(@Valid @NotNull MessageDTOs messageDTOs) {
        topicRepository.validateTopicIds(Arrays.asList(messageDTOs.getTopicId()));
        List<String> fqcns = new ArrayList<String>();
        List<ReturnMessage> success = new ArrayList<ReturnMessage>();
        List<ReturnMessage> failed = new ArrayList<ReturnMessage>();
        messageDTOs.getFqcns().forEach(fqcn -> {
            Optional.ofNullable(channelRepository.validateChannel(fqcn)).ifPresentOrElse(item -> {
                failed.add(item);
                this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(item));
            }, () -> {
                fqcns.add(fqcn);
            });
        });

        try {
            Connection nc = Nats.connect(natsJetstreamUrl);

            JetStream js = nc.jetStream();

            fqcns.forEach(fqcn -> {

                MessageDTO messageDTO = messageDTOs;
                messageDTO.setFqcn(fqcn);
                String id = messageRepository.save(messageDTO, DID);
                PublishOptions.Builder pubOptsBuilder = PublishOptions.builder()
                		.messageId(id).stream(messageDTO.streamName());

                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("messageId", id);
                builder.add("payloadEncryption", messageDTO.isPayloadEncryption());
                builder.add("payload", messageDTO.getPayload());
                builder.add("topicVersion", messageDTO.getTopicVersion());
                builder.add("transactionId", Optional.ofNullable(messageDTO.getTransactionId()).orElse(""));
                builder.add("sender", DID);
                builder.add("signature", messageDTO.getSignature());
                builder.add("clientGatewayMessageId", messageDTO.getClientGatewayMessageId());
                builder.add("timestampNanos", String.valueOf(TimeUnit.NANOSECONDS.toNanos(new Date().getTime())));

                builder.add("isFile", messageDTO.getIsFile());

                try {
                    PublishAck pa = js.publish(messageDTO.subjectName(),
                            builder.build().toString().getBytes(StandardCharsets.UTF_8),
                            (messageDTO.getTransactionId() != null) ? pubOptsBuilder.build() : null);
                    if(!pa.isDuplicate()) {
                    	ReturnMessage successMessage = new ReturnMessage();
                    	successMessage.setDid(fqcn);
                    	successMessage.setStatusCode(200);
                    	successMessage.setMessageId(id);
                    	success.add(successMessage);
                    }else {
                    	ReturnMessage errorMessage = new ReturnMessage();
                        errorMessage.setStatusCode(500);
                        errorMessage.setDid(fqcn);
                        errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", "Duplicate transaction id."));
                        failed.add(errorMessage);
                        this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(errorMessage));
                    }
                } catch (IOException | JetStreamApiException ex) {
                    ReturnMessage errorMessage = new ReturnMessage();
                    errorMessage.setStatusCode(500);
                    errorMessage.setDid(fqcn);
                    errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", ex.getMessage()));
                    failed.add(errorMessage);
                    this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(errorMessage));
                }
            });

            nc.flush(Duration.ZERO);
            nc.close();

        } catch (IOException | TimeoutException | InterruptedException ex) {
            ReturnMessage errorMessage = new ReturnMessage();
            errorMessage.setStatusCode(500);
            errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", ex.getMessage()));
            failed.add(errorMessage);
            this.logger.error("[" + DID + "]" + JsonbBuilder.create().toJson(errorMessage));
        }
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setClientGatewayMessageId(messageDTOs.getClientGatewayMessageId());
        messageResponse.setRecipients(new Recipients(messageDTOs.getFqcns().size(), success.size(), failed.size()));
        messageResponse.add(success, failed);
        return Response.ok().entity(messageResponse).build();

    }

    @Counted(name = "internal_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "internal_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @Path("internal")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = HashMap.class)))
    @Authenticated
    public Response publishInternal(@Valid @NotNull InternalMessageDTO internalMessageDTO)
            throws InterruptedException, JetStreamApiException, TimeoutException, IOException {

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFqcn(internalMessageDTO.getFqcn());
        messageDTO.setPayload(internalMessageDTO.getPayload());
        messageDTO.setClientGatewayMessageId(internalMessageDTO.getClientGatewayMessageId());
        messageDTO.setTopicId(internalTopicId);

        Connection nc = Nats.connect(natsJetstreamUrl);

        JetStream js = nc.jetStream();
        PublishOptions.Builder pubOptsBuilder = PublishOptions.builder()
                .messageId(messageDTO.getTransactionId());

        String id = messageRepository.save(messageDTO, DID);

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("messageId", id);
        builder.add("payload", messageDTO.getPayload());
        builder.add("transactionId", Optional.ofNullable(messageDTO.getTransactionId()).orElse(""));
        builder.add("sender", DID);
        builder.add("clientGatewayMessageId", messageDTO.getClientGatewayMessageId());
        builder.add("timestampNanos", String.valueOf(TimeUnit.NANOSECONDS.toNanos(new Date().getTime())));

        PublishAck pa = js.publish(messageDTO.subjectName(),
                builder.build().toString().getBytes(StandardCharsets.UTF_8),
                (messageDTO.getTransactionId() != null) ? pubOptsBuilder.build() : null);

        nc.flush(Duration.ZERO);
        nc.close();

        HashMap<String, String> map = new HashMap<>();
        map.put("messageId", id);
        return Response.ok().entity(map).build();
    }

    @Counted(name = "internal-search_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "internal-search_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @Path("internal/search")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = MessageDTO.class)))
    @Authenticated
    public Response searchInternal(@Valid @NotNull SearchInternalMessageDTO messageDTO)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        messageDTO.setFqcn(DID);

        List<MessageDTO> messageDTOs = new ArrayList<MessageDTO>();
        try {
            Connection nc = Nats.connect(natsJetstreamUrl);
            JetStream js = nc.jetStream();

            Builder builder = ConsumerConfiguration.builder().durable(messageDTO.findDurable());
            builder.maxAckPending(Duration.ofSeconds(5).toMillis());
            // builder.durable(messageDTO.getClientId()); // required

            JetStreamSubscription sub = js.subscribe(messageDTO.subjectName(internalTopicId),
                    builder.buildPullSubscribeOptions());
            nc.flush(Duration.ofSeconds(1));

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

                    HashMap<String, Object> natPayload = JsonbBuilder.create().fromJson(new String(m.getData()),
                            HashMap.class);

                    String sender = (String) natPayload.get("sender");

                    if (Optional.ofNullable(messageDTO.getFrom()).isPresent() &&
                            TimeUnit.NANOSECONDS.toNanos(Date.from(Optional.ofNullable(messageDTO.getFrom()).get()
                                    .atZone(ZoneId.systemDefault()).toInstant()).getTime()) > Long
                                            .valueOf((String) natPayload.get("timestampNanos")).longValue()) {
                        continue;
                    }

                    if (Optional.ofNullable(messageDTO.getSenderId()).isPresent() && messageDTO.getSenderId().stream()
                            .filter(id -> sender.contains(id)).findFirst().isEmpty()) {
                        continue;
                    }

                    MessageDTO message = new MessageDTO();
                    message.setPayload((String) natPayload.get("payload"));
                    message.setFqcn(messageDTO.getFqcn());
                    message.setId((String) natPayload.get("messageId"));
                    message.setSenderDid(sender);
                    message.setTimestampNanos(Long.valueOf((String) natPayload.get("timestampNanos")).longValue());
                    message.setClientGatewayMessageId((String) natPayload.get("clientGatewayMessageId"));
                    messageDTOs.add(message);
                    m.ack();
                }
            }
            // sub.unsubscribe();
            nc.close();
        } catch (IllegalArgumentException ex) {
            this.logger.warn("[SearchMessage][IllegalArgument][" + DID + "]" + ex.getMessage());
        }
        return Response.ok().entity(messageDTOs).build();
    }

    @Counted(name = "search_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "search_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @Path("search")
    @APIResponse(description = "", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = MessageDTO.class)))
    @Authenticated
    public Response search(@Valid @NotNull SearchMessageDTO messageDTO)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        topicRepository.validateTopicIds(messageDTO.getTopicId());
        // channelRepository.validateChannel(messageDTO.getFqcn(),topicId,DID);
        messageDTO.setFqcn(DID);

        List<MessageDTO> messageDTOs = new ArrayList<MessageDTO>();
        try {
            Connection nc = Nats.connect(natsJetstreamUrl);
            JetStream js = nc.jetStream();

            Builder builder = ConsumerConfiguration.builder().durable(messageDTO.findDurable());
            builder.maxAckPending(Duration.ofSeconds(15).toMillis());
            // builder.durable(messageDTO.getClientId()); // required

            JetStreamSubscription sub = js.subscribe(messageDTO.subjectAll(), builder.buildPullSubscribeOptions());
            nc.flush(Duration.ofSeconds(10));

            while (messageDTOs.size() < messageDTO.getAmount()) {
                List<io.nats.client.Message> messages = sub.fetch(messageDTO.getAmount(), Duration.ofSeconds(3));
                if (messages.isEmpty()) {
                    break;
                }
                for (io.nats.client.Message m : messages) {
                    m.inProgress();
                    if (m.isStatusMessage()) {
                        m.nak();
                        continue;
                    }

                    HashMap<String, Object> natPayload = JsonbBuilder.create().fromJson(new String(m.getData()),
                            HashMap.class);

                    String sender = (String) natPayload.get("sender");

                    if (Optional.ofNullable(messageDTO.getFrom()).isPresent() &&
                            TimeUnit.NANOSECONDS.toNanos(Date.from(Optional.ofNullable(messageDTO.getFrom()).get()
                                    .atZone(ZoneId.systemDefault()).toInstant()).getTime()) > Long
                                            .valueOf((String) natPayload.get("timestampNanos")).longValue()) {
                        continue;
                    }

                    if (messageDTO.getTopicId().stream().filter(id -> m.getSubject().contains(id)).findFirst()
                            .isEmpty()) {
                        continue;
                    }

                    if (messageDTO.getSenderId().stream().filter(id -> sender.contains(id)).findFirst().isEmpty()) {
                        continue;
                    }

                    MessageDTO message = new MessageDTO();
                    message.setPayload((String) natPayload.get("payload"));
                    message.setPayloadEncryption((boolean) natPayload.get("payloadEncryption"));
                    message.setFqcn(messageDTO.getFqcn());
                    message.setTopicId(m.getSubject().replaceFirst(DID.concat("."), ""));
                    message.setId((String) natPayload.get("messageId"));
                    message.setSenderDid(sender);
                    message.setTopicVersion((String) natPayload.get("topicVersion"));
                    message.setSignature((String) natPayload.get("signature"));
                    message.setTimestampNanos(Long.valueOf((String) natPayload.get("timestampNanos")).longValue());
                    message.setClientGatewayMessageId((String) natPayload.get("clientGatewayMessageId"));
                    message.setFromUpload((boolean) natPayload.get("isFile"));
                    message.setTransactionId((String) natPayload.get("transactionId"));

                    if (messageDTOs.size() < messageDTO.getAmount()) {
                        messageDTOs.add(message);
                        m.ack();
                    } else {
                        break;
                    }
                }
            }
            // sub.unsubscribe();
            nc.close();

        } catch (IllegalArgumentException ex) {
            this.logger.warn("[SearchMessage][IllegalArgument][" + DID + "]" + ex.getMessage());
        }
        this.logger.info("[SearchMessage][" + DID + "] result size " + messageDTOs.size());
        return Response.ok().entity(messageDTOs).build();
    }

    @Counted(name = "upload_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "upload_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @Authenticated
    public Response uploadFile(@Valid @MultipartForm FileUploadDTOs data, @HeaderParam("Authorization") String token) {
        topicRepository.validateTopicIds(Arrays.asList(data.getTopicId()));
        data.setOwnerdid(DID);
        String fileId = fileUploadRepository.save(data, channelRepository.findByFqcn(DID));
        data.setFileName(fileId);
        return Response.ok()
                .entity(producerTemplate.sendBodyAndProperty("direct:azureupload", ExchangePattern.InOut, data, "token",
                        token))
                .build();
    }

    @Counted(name = "uploadc_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "uploadc_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @Path("uploadc")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @Authenticated
    public Response uploadFileChunks(@Valid @MultipartForm FileUploadChunkDTOs data,
            @HeaderParam("Authorization") String token) {

        Double chunks = Math.ceil(data.getFileSize() / data.getChunkSize());
        File tempFile = new File(FileUtils.getTempDirectory(), data.getClientGatewayMessageId() + ".enc");

        if (data.getCurrentChunkIndex() == 0 && tempFile.length() != 0l) {
            FileUtils.deleteQuietly(tempFile);
            tempFile = new File(FileUtils.getTempDirectory(), data.getClientGatewayMessageId() + ".enc");
        }

        try {
            FileUtils.writeByteArrayToFile(tempFile, data.getFile().readAllBytes(), true);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (data.getCurrentChunkIndex() == (chunks - 1)) {
            try {
                String checksum = DigestUtils.sha256Hex(FileUtils.openInputStream(tempFile));
                data.setFile(FileUtils.openInputStream(tempFile));
                if (checksum.compareTo(data.getFileChecksum()) != 0) {
                    ReturnMessage errorMessage = new ReturnMessage();
                    errorMessage.setStatusCode(400);
                    errorMessage.setDid(data.getFqcn());
                    errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", "Checksum failed"));

                    MessageResponse messageResponse = new MessageResponse();
                    messageResponse.setClientGatewayMessageId(data.getClientGatewayMessageId());
                    messageResponse.setRecipients(new Recipients(1, 0, 1));
                    messageResponse.add(Arrays.asList(), Arrays.asList(errorMessage));
                    return Response.ok().entity(messageResponse).build();
                }
            } catch (IOException e) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } finally {
                FileUtils.deleteQuietly(tempFile);
            }
            return this.uploadFile(data, token);
        } else {
            return Response.ok().build();
        }
    }

    @Counted(name = "download_get_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "download_get_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
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
                .header("clientGatewayMessageId", messageDTO.getClientGatewayMessageId())
                .header("payloadEncryption", messageDTO.isPayloadEncryption())
                .header("ownerDid", messageDTO.getSenderDid())
                .header("signature", messageDTO.getSignature())
                .build();

    }
}