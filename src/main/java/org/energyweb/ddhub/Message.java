package org.energyweb.ddhub;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
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
import org.energyweb.ddhub.dto.MessageAckDTO;
import org.energyweb.ddhub.dto.MessageAckDTOs;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.MessageDTOs;
import org.energyweb.ddhub.dto.SearchInternalMessageDTO;
import org.energyweb.ddhub.dto.SearchMessageDTO;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.energyweb.ddhub.helper.MessageResponse;
import org.energyweb.ddhub.helper.Recipients;
import org.energyweb.ddhub.helper.ReturnErrorMessage;
import org.energyweb.ddhub.helper.ReturnMessage;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.FileUploadRepository;
import org.energyweb.ddhub.repository.TopicRepository;
import org.energyweb.ddhub.repository.TopicVersionRepository;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamOptions;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PublishOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerConfiguration.Builder;
import io.nats.client.api.PublishAck;
import io.opentelemetry.extension.annotations.WithSpan;
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
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "roles")
    String roles;

    @ConfigProperty(name = "INTERNAL_TOPIC")
    String internalTopicId;

    @ConfigProperty(name = "INTERNAL_POSTFIX_CLIENT_ID", defaultValue = "20220510")
    String clientIdPostfix;

    @HeaderParam("X-Request-Id")
    String requestId;

    @Counted(name = "messages_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "messages_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @Authenticated
    public Response publish(@Valid @NotNull MessageDTOs messageDTOs)
            throws InterruptedException, TimeoutException, IOException {
    	if(messageDTOs.anonymousRule()) throw new IllegalArgumentException(messageDTOs.anonymousRuleErrorMsg());
        topicRepository.validateTopicIds(Arrays.asList(messageDTOs.getTopicId()));
        List<String> fqcns = new ArrayList<String>();
        List<ReturnMessage> success = new ArrayList<ReturnMessage>();
        List<ReturnMessage> failed = new ArrayList<ReturnMessage>();
        failed.addAll(messageDTOs.validateFqcnParam());
        failed.addAll(messageDTOs.validateAnonymousRecipientParam());
        messageDTOs.findFqcnList().forEach(fqcn -> {
            Optional.ofNullable(channelRepository.validateChannel(fqcn)).ifPresentOrElse(item -> {
                failed.add(item);
                this.logger.error("[PUBLISH][" + DID + "][" + requestId + "]" + JsonbBuilder.create().toJson(item));
            }, () -> {
                fqcns.add(fqcn);
            });
        });
        Connection nc = null;
        HashSet<String> messageIds = new HashSet<String>();
        List<CompletableFuture<PublishAck>> futures = new ArrayList<>();
        HashMap<CompletableFuture<PublishAck>,MessageDTO> futuresMap = new HashMap<>();
        try {
            nc = Nats.connect(natsConnectionOption());
            
            JetStream js = nc.jetStream(natsJetStreamOption());
            fqcns.forEach(fqcn -> {

                MessageDTO messageDTO = messageDTOs;
                messageDTO.setFqcn(fqcn);
                messageDTO.setSenderDid(DID);
                String id = new ObjectId().toHexString();
                
                PublishOptions.Builder pubOptsBuilder = PublishOptions.builder()
                        .messageId(messageDTO.createNatsTransactionId()).stream(messageDTO.streamName());

                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("messageId", id);
                builder.add("payloadEncryption", messageDTO.isPayloadEncryption());
                builder.add("payload", messageDTO.getPayload());
                builder.add("topicVersion", messageDTO.getTopicVersion());
                builder.add("transactionId", Optional.ofNullable(messageDTO.getTransactionId()).orElse(""));
                builder.add("sender", DID);
                builder.add("signature", messageDTO.getSignature());
                builder.add("clientGatewayMessageId", messageDTO.getClientGatewayMessageId());
                builder.add("timestampNanos", String.valueOf(TimeUnit.MILLISECONDS.toNanos(new Date().getTime())));

                builder.add("isFile", messageDTO.getIsFile());

                CompletableFuture<PublishAck> pa = js.publishAsync(messageDTO.subjectName(),
                		builder.build().toString().getBytes(StandardCharsets.UTF_8),
                		(messageDTO.getTransactionId() != null) ? pubOptsBuilder.build() : null);
                futures.add(pa);
                MessageDTO _messageDTO = new MessageDTO();
                _messageDTO.setFqcn(fqcn);
                _messageDTO.setId(id);
                futuresMap.put(pa, _messageDTO);
                
            });
            
            while (futures.size() > 0) {
            	CompletableFuture<PublishAck> f = futures.remove(0);
                if (f.isDone()) {
                    try {
                        PublishAck pa = f.get();
                        MessageDTO messageDTO = futuresMap.get(f);
                        if (!pa.isDuplicate()) {
                            ReturnMessage successMessage = new ReturnMessage();
                            successMessage.setDid(messageDTO.getFqcn());
                            successMessage.setStatusCode(200);
                            successMessage.setMessageId(messageDTO.getId());
                            success.add(successMessage);
                            messageIds.add(messageDTO.getId());
                        } else {
                            ReturnMessage errorMessage = new ReturnMessage();
                            errorMessage.setStatusCode(400);
                            errorMessage.setDid(messageDTO.getFqcn());
                            errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", "Duplicate transaction id."));
                            failed.add(errorMessage);
                            this.logger
                                    .error("[PUBLISH][1][" + DID + "][" + requestId + "]"
                                            + JsonbBuilder.create().toJson(errorMessage));
                        }
                    }
                    catch (ExecutionException ex) {
                    	ReturnMessage errorMessage = new ReturnMessage();
                        errorMessage.setStatusCode(400);
                        errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", ex.getMessage()));
                        failed.add(errorMessage);
                        this.logger.error("[PUBLISH][2][" + DID + "][" + requestId + "]"
                                + JsonbBuilder.create().toJson(errorMessage));
                    }
                }
                else {
                    futures.add(f);
                }
            }
            
            futuresMap.clear();

        } catch (InterruptedException ex) {
            ReturnMessage errorMessage = new ReturnMessage();
            errorMessage.setStatusCode(400);
            errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", ex.getMessage()));
            failed.add(errorMessage);
            this.logger
                    .error("[PUBLISH][3][" + DID + "][" + requestId + "]" + JsonbBuilder.create().toJson(errorMessage));
        } finally {
            if (nc != null) {
                nc.flush(Duration.ZERO);
                nc.close();
            }
        }

        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setClientGatewayMessageId(messageDTOs.getClientGatewayMessageId());
        messageResponse.setRecipients(new Recipients(messageDTOs.findFqcnList().size(), success.size(), failed.size()));
        messageResponse.add(success, failed);

        this.logger.info("[PUBLISH][" + DID + "][" + requestId + "] result success messageIds : " + messageIds);
        messageIds.clear();

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

    	Connection nc = null;
    	HashMap<String, String> map = new HashMap<>();
    	List<CompletableFuture<PublishAck>> futures = new ArrayList<>();
        HashMap<CompletableFuture<PublishAck>,MessageDTO> futuresMap = new HashMap<>();
        
        try {
        	MessageDTO messageDTO = new MessageDTO();
            messageDTO.setFqcn(internalMessageDTO.getFqcn() + ".keys");
            messageDTO.setPayload(internalMessageDTO.getPayload());
            messageDTO.setClientGatewayMessageId(internalMessageDTO.getClientGatewayMessageId());
            messageDTO.setTopicId(internalTopicId);

            nc = Nats.connect(natsConnectionOption());

            JetStream js = nc.jetStream(natsJetStreamOption());

            String id = UUID.randomUUID().toString();

            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("messageId", id);
            builder.add("payload", messageDTO.getPayload());
            builder.add("transactionId", Optional.ofNullable(messageDTO.getTransactionId()).orElse(""));
            builder.add("sender", DID);
            builder.add("clientGatewayMessageId", messageDTO.getClientGatewayMessageId());
            builder.add("timestampNanos", String.valueOf(TimeUnit.MILLISECONDS.toNanos(new Date().getTime())));

            
            CompletableFuture<PublishAck> pa = js.publishAsync(messageDTO.subjectName(),
            		builder.build().toString().getBytes(StandardCharsets.UTF_8));
            futures.add(pa);
            MessageDTO _messageDTO = new MessageDTO();
            _messageDTO.setFqcn(messageDTO.getFqcn());
            _messageDTO.setId(id);
            futuresMap.put(pa, _messageDTO);
            
            
            while (futures.size() > 0) {
            	CompletableFuture<PublishAck> f = futures.remove(0);
                if (f.isDone()) {
                    try {
                        PublishAck _pa = f.get();
                        map.put("messageId", id);
                    }
                    catch (ExecutionException ex) {
                    	ReturnMessage errorMessage = new ReturnMessage();
                        errorMessage.setStatusCode(400);
                        errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", ex.getMessage()));
                        this.logger.error("[PUBLISH][KEYS][1][" + DID + "][" + requestId + "]"
                                + JsonbBuilder.create().toJson(errorMessage));
                    }
                }
                else {
                    futures.add(f);
                }
            }
            
            futuresMap.clear();

        } catch (InterruptedException ex) {
            ReturnMessage errorMessage = new ReturnMessage();
            errorMessage.setStatusCode(400);
            errorMessage.setErr(new ReturnErrorMessage("MB::NATS_SERVER", ex.getMessage()));
            this.logger
                    .error("[PUBLISH][KEYS][2][" + DID + "][" + requestId + "]" + JsonbBuilder.create().toJson(errorMessage));
        } finally {
            if (nc != null) {
                nc.flush(Duration.ZERO);
                nc.close();
            }
        }
        
        this.logger.info("[PUBLISH][KEYS][" + DID + "][" + requestId + "] result success messageId : " + map.get("messageId"));
        
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
        messageDTO.setFqcn(DID + ".keys");

        HashSet<io.nats.client.Message> messageNats = new HashSet<io.nats.client.Message>();
        HashSet<MessageDTO> messageDTOs = new HashSet<MessageDTO>();
        HashSet<String> messageIds = new HashSet<String>();
        Connection nc = null;
        try {
            nc = Nats.connect(natsConnectionOption());
            JetStream js = nc.jetStream(natsJetStreamOption());

            Builder builder = ConsumerConfiguration.builder().durable(messageDTO.findDurable());
            builder.maxAckPending(50000);

            JetStreamSubscription sub = js.subscribe(messageDTO.subjectName(internalTopicId),
                    builder.buildPullSubscribeOptions());
            nc.flush(Duration.ofSeconds(1));

            boolean isHadMessages = sub.getConsumerInfo().getNumAckPending() > 0 || sub.getConsumerInfo().getNumPending() > 0;
            boolean isDuplicate = false;
            
            while (isHadMessages && messageDTOs.size() < messageDTO.getAmount() && sub != null && sub.isActive()) {
            	List<io.nats.client.Message> messages = sub.fetch(messageDTO.fetchAmount(sub.getConsumerInfo().getNumAckPending()), Duration.ofSeconds(3));
                if (messages.isEmpty()) {
                    break;
                }
                messages.sort((a, b) -> (a.metaData().streamSequence() >= b.metaData().streamSequence())? 1:-1);
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
                            TimeUnit.SECONDS.toNanos(messageDTO.getFrom().toEpochSecond(ZoneOffset.UTC)) > Long
                                            .valueOf((String) natPayload.get("timestampNanos")).longValue()) {
                        continue;
                    }

                    if (Optional.ofNullable(messageDTO.getSenderId()).isPresent() && messageDTO.getSenderId().stream()
                            .filter(id -> sender.contains(id)).findFirst().isEmpty()) {
                        continue;
                    }

                    MessageDTO message = new MessageDTO();
                    message.setPayload((String) natPayload.get("payload"));
                    message.setFqcn(DID);
                    message.setId((String) natPayload.get("messageId"));
                    message.setSenderDid(sender);
                    message.setTimestampNanos(Long.valueOf((String) natPayload.get("timestampNanos")).longValue());
                    message.setClientGatewayMessageId((String) natPayload.get("clientGatewayMessageId"));

                    if (messageDTOs.size() < messageDTO.getAmount()) {
                    	m.inProgress();

                        if (!messageIds.contains(message.getId())) {
                            messageDTOs.add(message);
                            messageIds.add(message.getId());
                            messageNats.add(m);
                        } else {
                            this.logger.warn(
                                    "[SearchMessage][KEYS][" + DID + "][" + requestId + "] Duplicate " + message.getId());
                            isDuplicate = true;
                            if (isDuplicate) {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                    
                    natPayload.clear();
                    natPayload = null;
                }
                if (messageDTOs.size() == messageDTO.getAmount()) {
                    break;
                }
                if (isDuplicate) {
                	break;
                }
            }
        } catch (TimeoutException ex) {
            this.logger.error("[SearchMessage][KEYS][TimeoutException][" + DID + "][" + requestId + "]" + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            this.logger.error("[SearchMessage][KEYS][IllegalArgument][" + DID + "][" + requestId + "]" + ex.getMessage());
        } finally {
            if (nc != null) {
            	nc.flush(Duration.ofSeconds(0));
            	messageNats.forEach(m -> m.ack());
                nc.close();
                messageNats.clear();
                messageIds.clear();
            }
        }
        
        this.logger.info(
                "[SearchMessage][KEYS][" + DID + "][" + requestId + "] SearchMessage keys result size " + messageDTOs.size());

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
            throws InterruptedException, TimeoutException, IOException, JetStreamApiException {
        topicRepository.validateTopicIds(messageDTO.getFqcnTopicList(), true);
        messageDTO.setFqcn(messageDTO.anonymousFqcnRule(DID));

        HashSet<io.nats.client.Message> messageNats = new HashSet<io.nats.client.Message>();
        List<io.nats.client.Message> acks = new ArrayList<io.nats.client.Message>();
        HashSet<MessageDTO> messageDTOs = new HashSet<MessageDTO>();
        HashSet<String> messageIds = new HashSet<String>();
        Connection nc = null;
        try {
            nc = Nats.connect(natsConnectionOption());
            JetStream js = nc.jetStream(natsJetStreamOption());
            
            messageDTO.manageSearchDateClientId(nc.jetStreamManagement());

            Builder builder = ConsumerConfiguration.builder().durable(messageDTO.findDurable());
            builder.maxAckPending(50000);
            builder.ackWait(Duration.ofSeconds(1));

            JetStreamSubscription sub = js.subscribe(messageDTO.subjectAll(), builder.buildPullSubscribeOptions());
            nc.flush(Duration.ofSeconds(0));

            boolean isHadMessages = sub.getConsumerInfo().getNumAckPending() > 0 || sub.getConsumerInfo().getNumPending() > 0;
            boolean isDuplicate = false;
            long totalAckPending = sub.getConsumerInfo().getNumAckPending();
            List<io.nats.client.Message> totalPendingAck = findAllAckPending(messageDTO, sub, totalAckPending);
            if(totalAckPending > 0 && totalAckPending != totalPendingAck.size()) {
                this.logger.warn("[SearchMessage][TotalAckPendingRetrieve][" + DID + "][" + requestId + "] Not able to retrieve complete TotalAckPendingRetrieve.");
                isHadMessages = false;
            }
            
            while (isHadMessages && messageDTOs.size() < messageDTO.getAmount() && sub != null && sub.isActive()) {
                List<io.nats.client.Message> messages = new ArrayList<io.nats.client.Message>();
                if(!totalPendingAck.isEmpty()) {
                    messages.addAll(totalPendingAck);
                    this.logger.info("[SearchMessage][" + DID + "][" + requestId + "] SearchMessage total for process size " + messages.size() + "/" + totalAckPending);
                    totalPendingAck.clear();
                    messages.sort((a, b) -> (a.metaData().streamSequence() >= b.metaData().streamSequence())? 1:-1);
                }else {
                    int _amount = messageDTO.getAmount();
                    if(messageDTOs.size() > 0) {
                        _amount = messageDTO.getAmount() - messageDTOs.size();
                    }
                    _amount = (_amount > MessageAckDTOs.MAX_FETCH_AMOUNT)?MessageAckDTOs.MAX_FETCH_AMOUNT:_amount;
                    messages = sub.fetch(_amount, Duration.ofSeconds(3));
                    this.logger.info("[SearchMessage][" + DID + "][" + requestId + "] SearchMessage messages " + messages.size());
                }
                
                if (messages.isEmpty()) {
                    break;
                }
                for (io.nats.client.Message m : messages) {
                    if (m.isStatusMessage()) {
                        m.nak();
                        acks.remove(m);
                        continue;
                    }

                    HashMap<String, Object> natPayload = JsonbBuilder.create().fromJson(new String(m.getData()),
                            HashMap.class);

                    String sender = (String) natPayload.get("sender");
                    
                    if (Optional.ofNullable(messageDTO.getFrom()).isPresent() &&
                            TimeUnit.SECONDS.toNanos(messageDTO.getFrom().toEpochSecond(ZoneOffset.UTC)) > Long
                                            .valueOf((String) natPayload.get("timestampNanos")).longValue()) {
                        m.ack();
                        acks.remove(m);
                        natPayload.clear();
                        natPayload = null;
                        
                        continue;
                    }

                    if (messageDTO.getFqcnTopicList().stream().filter(id -> m.getSubject().contains(id)).findFirst().isEmpty()) {
                        m.ack();
                        acks.remove(m);
                        natPayload.clear();
                        natPayload = null;
                    	continue;
                    }

                    if (messageDTO.getSenderId().stream().filter(id -> sender.contains(id)).findFirst().isEmpty()) {
                    	m.ack();
                    	acks.remove(m);
                    	natPayload.clear();
                        natPayload = null;
                        continue;
                    }
                    
                    
                    if (messageDTO.getTopicId() != null && !messageDTO.getTopicId().isEmpty() && messageDTO.getTopicId().stream().filter(id -> m.getSubject().contains(id)).findFirst().isEmpty()) {
                        messageNats.add(m);
                        natPayload.clear();
                        natPayload = null;
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
                        if (messageDTO.isAck()) {
                            m.ack();
                            acks.remove(m);
                        }

                        if (!messageIds.contains(message.getId())) {
                            messageDTOs.add(message);
                            messageIds.add(message.getId());
                            messageNats.add(m);
                        } else {
                            this.logger.warn(
                                    "[SearchMessage][" + DID + "][" + requestId + "] Duplicate " + message.getId());
                            isDuplicate = true;
                            if (isDuplicate) {
                                messages.removeAll(messageNats);
                                messages.removeAll(acks);
                                messageNats.addAll(messages);
                                natPayload.clear();
                                natPayload = null;
                                break;
                            }
                        }
                    } else {
                        messages.removeAll(messageNats);
                        messages.removeAll(acks);
                        messageNats.addAll(messages);
                        natPayload.clear();
                        natPayload = null;
                        break;
                    }
                    
                    natPayload.clear();
                    natPayload = null;
                }
                
                if (messageDTOs.size() == messageDTO.getAmount() || isDuplicate) {
                    this.logger.info("[SearchMessage][" + DID + "][" + requestId + "] SearchMessage result size " + messageDTOs.size());
                    break;
                }
            }
        } catch (TimeoutException ex) {
            this.logger.error("[SearchMessage][TimeoutException][" + DID + "][" + requestId + "]" + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            this.logger.error("[SearchMessage][IllegalArgument][" + DID + "][" + requestId + "]" + ex.getMessage());
        } finally {
            if (nc != null) {
            	nc.flush(Duration.ofSeconds(0));
            	this.logger.info("[SearchMessage][" + DID + "][" + requestId + "] SearchMessage messageNats size " + messageNats.size());
                messageNats.forEach(m -> {
                	m.nak();
                });
                nc.close();
                
                messageNats.clear();
                acks.clear();
            }
        }

        this.logger.info("[SearchMessage][" + DID + "][" + requestId + "] SearchMessage result size " + messageDTOs.size());
        this.logger.info("[SearchMessage][" + DID + "][" + requestId + "] SearchMessage result messageIds : " + messageIds);
        
        messageIds.clear();

        return Response.ok().entity(messageDTOs).build();
    }

    @Counted(name = "ack_post_count", description = "", tags = { "ddhub=messages" }, absolute = true)
    @Timed(name = "ack_post_timed", description = "", tags = {
            "ddhub=messages" }, unit = MetricUnits.MILLISECONDS, absolute = true)
    @POST
    @Path("ack")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = MessageAckDTO.class)))
    @Authenticated
    public Response natsAck(@Valid @NotNull MessageAckDTOs ackDTOs)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        SearchMessageDTO messageDTO = new SearchMessageDTO();
        messageDTO.setAnonymousRecipient(ackDTOs.getAnonymousRecipient());
        messageDTO.setFqcn(messageDTO.anonymousFqcnRule(DID));
        messageDTO.setClientId(ackDTOs.getClientId());
        messageDTO.setAmount(ackDTOs.getMessageIds().size());
        messageDTO.setFrom(ackDTOs.getFrom());
        HashSet<String> messageIds = new HashSet<String>();
        Connection nc = null;
        boolean isTotalAckPendingRetrieve = false;
        try {
            nc = Nats.connect(natsConnectionOption());
            JetStream js = nc.jetStream(natsJetStreamOption());
            

            Builder builder = ConsumerConfiguration.builder().durable(messageDTO.findDurable());
            builder.maxAckPending(50000);
            builder.ackWait(Duration.ofSeconds(1));

            JetStreamSubscription sub = js.subscribe(messageDTO.subjectAll(), builder.buildPullSubscribeOptions());
            long totalAckPending = sub.getConsumerInfo().getNumAckPending();
            long totalNumPending = sub.getConsumerInfo().getNumPending();
            
            List<io.nats.client.Message> totalPendingAck = new ArrayList<io.nats.client.Message>();
            if(messageDTO.getFrom() != null && !messageDTO.checkConsumerExist(nc.jetStreamManagement(), messageDTO.streamName(), messageDTO.findDurable())) {
            	totalAckPending = 0;
            	totalNumPending = 0;
            	isTotalAckPendingRetrieve = true;
            }else {
            	totalPendingAck = findAllAckPending(messageDTO, sub, totalAckPending);
            	isTotalAckPendingRetrieve = totalAckPending == totalPendingAck.size();
            	if(totalNumPending > 0) {
            		isTotalAckPendingRetrieve = totalNumPending <= sub.getConsumerInfo().getNumPending();
            	}
            }
            
            totalPendingAck.sort((a, b) -> (a.metaData().streamSequence() >= b.metaData().streamSequence())? 1:-1);
            
            for (io.nats.client.Message m : totalPendingAck) {

                if (m.isStatusMessage()) {
                    m.nak();
                    continue;
                }

                HashMap<String, Object> natPayload = JsonbBuilder.create().fromJson(new String(m.getData()), HashMap.class);

                String messageId = (String) natPayload.get("messageId");

                if (!ackDTOs.getMessageIds().contains(messageId)) {
                    m.nak();
                    natPayload.clear();
                    natPayload = null;
                    continue;
                }


                if (messageIds.size() < messageDTO.getAmount()) {
                    m.ack();
                    this.logger.info("[NatsAck][" + DID + "][" + requestId + "] NatsAck for " + messageId);
                    if (!messageIds.contains(messageId)) {
                        messageIds.add(messageId);
                    } else {
                        this.logger.warn("[NatsAck][" + DID + "][" + requestId + "] Duplicate " + messageId);
                    }
                }
                natPayload.clear();
                natPayload = null;
            }
            
        } catch (IllegalArgumentException ex) {
            this.logger.error("[NatsAck][IllegalArgument][" + DID + "][" + requestId + "]" + ex.getMessage());
        } finally {
            if (nc != null) {
            	nc.flush(Duration.ofSeconds(0));
                nc.close();
            }
        }
        
        MessageAckDTO ackDTO = new MessageAckDTO();
        ackDTO.setAcked(new ArrayList<>(messageIds));
        if(isTotalAckPendingRetrieve) {
        	List<String> notFound = ackDTOs.getMessageIds();
        	notFound.removeAll(ackDTO.getAcked());
        	ackDTO.setNotFound(notFound);
        }
        
        this.logger.info("[NatsAck][" + DID + "][" + requestId + "] NatsAck result size " + messageIds.size());

        messageIds.clear();
        messageIds = null;
        
        if(!isTotalAckPendingRetrieve) {
            this.logger.error("[NatsAck][TotalAckPendingRetrieve][" + DID + "][" + requestId + "] Not able to retrieve complete TotalAckPendingRetrieve.");
            return Response.status(400).entity(new ErrorResponse("20", "Not able to retrieve complete TotalAckPendingRetrieve.")).build();
        }
            
        return Response.ok().entity(ackDTO).build();
    }

    @WithSpan("findAllAckPending")
	private List<io.nats.client.Message> findAllAckPending(SearchMessageDTO messageDTO, JetStreamSubscription sub, long totalAckPending)
	        throws IOException, JetStreamApiException, InterruptedException {
	    
	    if(totalAckPending == 0) return new ArrayList<>();
	    HashSet<io.nats.client.Message> totalPendingAck = new HashSet<io.nats.client.Message>();
	    int emptyMessagesCounter = 0;
	    this.logger.info("[FindAllAckPending][" + DID + "][" + requestId + "] FindAllAckPending size totalAckPending : " + totalAckPending);
	    while (totalPendingAck.size() < totalAckPending && sub != null && sub.isActive()) {
	        long _amount = totalAckPending;
	        if(totalPendingAck.size() > 0) {
	            _amount = totalAckPending - totalPendingAck.size();
	        }
	        _amount = (_amount > MessageAckDTOs.MAX_FETCH_AMOUNT)?MessageAckDTOs.MAX_FETCH_AMOUNT:_amount;
	        List<io.nats.client.Message> messages = sub.fetch((int)_amount, Duration.ofSeconds(3));
	        this.logger.info("[FindAllAckPending][" + DID + "][" + requestId + "] FindAllAckPending messages size " + messages.size() + "/" + totalAckPending);
	        totalPendingAck.addAll(messages);
	        
	        if (messages.isEmpty()) {
	            this.logger.warn("[FindAllAckPending][" + DID + "][" + requestId + "] FindAllAckPending totalPendingAck : empty return.");
	            emptyMessagesCounter +=1;
	            Thread.sleep(Duration.ofMillis(500).toMillis());
	            messages = sub.fetch(messageDTO.fetchAmount(_amount), Duration.ofSeconds(3));
	            totalPendingAck.addAll(messages);
	            if(emptyMessagesCounter >= 3) {
	                this.logger.info("[FindAllAckPending][" + DID + "][" + requestId + "] FindAllAckPending unmatch totalPendingAck size " + totalPendingAck.size() + "/" + totalAckPending);
	                break;
	            }
	        }
	        
	        if(totalPendingAck.size() == totalAckPending ) {
	            this.logger.info("[FindAllAckPending][" + DID + "][" + requestId + "] FindAllAckPending match totalPendingAck size " + totalPendingAck.size() + "/" + totalAckPending);
	            break;
	        }
	    }
	    return new ArrayList<>(totalPendingAck);
	}

	private JetStreamOptions natsJetStreamOption() {
        return JetStreamOptions.builder().requestTimeout(Duration.ofSeconds(MessageDTO.REQUEST_TIMEOUT)).build();
    }

    private Options natsConnectionOption() {
        return new Options.Builder().server(natsJetstreamUrl).maxReconnects(MessageDTO.MAX_RECONNECTS)
                .connectionTimeout(Duration.ofSeconds(MessageDTO.CONNECTION_TIMEOUT)). // Set timeout
                build();
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