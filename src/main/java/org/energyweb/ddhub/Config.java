package org.energyweb.ddhub;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import io.nats.client.JetStreamApiException;
import io.quarkus.security.Authenticated;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(value = @Tag(name = "Internal", description = "All the methods"))
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "AuthServer", type = SecuritySchemeType.HTTP, scheme = "Bearer") })
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class Config {

    @Inject
    Logger logger;

    @ConfigProperty(name = "NATS_MAX_AGE")
    long natsMaxAge;

    @ConfigProperty(name = "NATS_MAX_SIZE")
    long natsMaxSize;

    @GET
    @Counted(name = "configuration_get_count", description = "", tags = {"ddhub=config"}, absolute = true)
    @Timed(name = "configuration_get_timed", description = "", tags = {"ddhub=config"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = Map.class)))
    @Authenticated
    public Response configuration() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        Map config = new HashMap<>();
        config.put("msg-expired", natsMaxAge);
        config.put("msg-max-size", natsMaxSize);
        return Response.ok().entity(config).build();

    }
}