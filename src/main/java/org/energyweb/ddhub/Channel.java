package org.energyweb.ddhub;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.energyweb.ddhub.dto.ChannelDTO;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;

@Path("/channel")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Channel {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "NATS_JS_URL")
    String natsJetstreamUrl;

    @PATCH
    public Response updateChannel(@Valid @NotNull ChannelDTO channelDTO)
            throws IOException, JetStreamApiException, InterruptedException, TimeoutException {
        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        StreamInfo _streamInfo = jsm.getStreamInfo(channelDTO.getStreamName());
        StreamConfiguration streamConfig = StreamConfiguration.builder(_streamInfo.getConfiguration())
                .addSubjects(channelDTO.getTopic())
                .maxAge(Duration.ofMillis(channelDTO.getMaxMsgAge()))
                .maxMsgSize(channelDTO.getMaxMsgSize())
                .duplicateWindow(0)
                .build();

        StreamInfo streamInfo = jsm.updateStream(streamConfig);
        nc.close();
        return Response.ok().entity(streamInfo).build();
    }

    @POST
    public Response createChannel(@Valid @NotNull ChannelDTO channelDTO)
            throws IOException, InterruptedException, ExecutionException, TimeoutException, JetStreamApiException {
        Connection nc = Nats.connect(natsJetstreamUrl);

        JetStreamManagement jsm = nc.jetStreamManagement();
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(channelDTO.getStreamName())
                .subjects(channelDTO.getSubjectName())
                .maxAge(Duration.ofMinutes(channelDTO.getMaxMsgAge()))
                .maxMsgSize(channelDTO.getMaxMsgSize())
                .duplicateWindow(0)
                .build();
        StreamInfo streamInfo = jsm.addStream(streamConfig);
        nc.close();
        return Response.ok().entity(streamInfo).build();
    }

    @GET
    @Path("pubsub")
    public Response listOfChannel() {
        return Response.ok().entity("Success").build();

    }

    @GET
    @Path("{fqcn}")
    public Response channelByfqcn(@PathParam("fqcn") String fqcn)
            throws IOException, InterruptedException, JetStreamApiException {
        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setFqcn(fqcn);
        StreamInfo _streamInfo = jsm.getStreamInfo(channelDTO.getStreamName());
        nc.close();
        return Response.ok().entity(_streamInfo).build();
    }

    @DELETE
    @Path("{fqcn}")
    public Response deletefqcn(@PathParam("fqcn") String fqcn)
            throws IOException, JetStreamApiException, InterruptedException {
        Connection nc = Nats.connect(natsJetstreamUrl);
        JetStreamManagement jsm = nc.jetStreamManagement();
        jsm.deleteStream(fqcn);
        nc.close();
        return Response.ok().entity("Success").build();
    }

}