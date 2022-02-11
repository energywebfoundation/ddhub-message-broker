package org.energyweb.ddhub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.MultipartBody;
import org.energyweb.ddhub.dto.ResponseMessage;
import org.energyweb.ddhub.helper.ErrorResponse;
import org.energyweb.ddhub.repository.ChannelRepository;
import org.energyweb.ddhub.repository.TopicRepository;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.PublishOptions;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.PublishAck;

@Path("/message")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
    ChannelRepository channelRepository;

    @POST
    public Response publish(@Valid @NotNull MessageDTO messageDTO)
            throws InterruptedException, JetStreamApiException, TimeoutException {
        topicRepository.validateTopicIds(Arrays.asList(messageDTO.getTopicId()));
        channelRepository.validateChannel(messageDTO.getFqcn());

        Connection nc;
        try {
            nc = Nats.connect(natsJetstreamUrl);
            JetStream js = nc.jetStream();
            PublishOptions.Builder pubOptsBuilder = PublishOptions.builder()
                    .messageId(messageDTO.getCorrelationId());
            PublishAck pa = js.publish(messageDTO.subjectName(),
                    messageDTO.getPayload().getBytes(StandardCharsets.UTF_8),
                    (messageDTO.getCorrelationId() != null) ? pubOptsBuilder.build() : null);

            nc.flush(Duration.ZERO);
            nc.close();
            return Response.ok().entity(pa).build();
        } catch (IOException e) {
            return Response.status(400).entity(new ErrorResponse("10", e.getMessage())).build();
        } catch (InterruptedException e) {
            return Response.status(400).entity(new ErrorResponse("10", e.getMessage())).build();
        }
    }

    @GET
    public Response pull(@NotNull @QueryParam("fqcn") String fqcn,
    		@Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") @NotNull @QueryParam("topicId") String topicId,
            @DefaultValue("default") @QueryParam("clientId") String clientId,
            @DefaultValue("1") @QueryParam("amount") Integer amount)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        topicRepository.validateTopicIds(Arrays.asList(topicId));
        channelRepository.validateChannel(fqcn);

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
        List<ResponseMessage> messageDTOs = new ArrayList<ResponseMessage>();
        for (io.nats.client.Message m : messages) {
            ResponseMessage messageDTO = new ResponseMessage();
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
    public Response uploadFile(@Valid @MultipartForm MultipartBody data) {
        topicRepository.validateTopicIds(Arrays.asList(data.getTopicId()));
        channelRepository.validateChannel(data.getFqcn());
        
        return Response.ok().entity(producerTemplate.sendBody("direct:azureupload", ExchangePattern.InOut, data))
                .build();
    }

    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@NotNull @QueryParam("fqcn") String fqcn,
    		@Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") @NotNull @QueryParam("topicId") String topicId,  @NotNull @QueryParam("filename") String filename) {
        
    	topicRepository.validateTopicIds(Arrays.asList(topicId));
        channelRepository.validateChannel(fqcn);
        
    	MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFqcn(fqcn);
        messageDTO.setTopicId(topicId);
        
    	return Response
                .ok(producerTemplate.sendBodyAndHeader("direct:azuredownload", ExchangePattern.InOut, null,
                        "CamelAzureStorageBlobBlobName", messageDTO.storageName() + filename), MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();

    }
}