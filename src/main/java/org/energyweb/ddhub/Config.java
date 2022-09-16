package org.energyweb.ddhub;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.math.NumberUtils;
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
import org.energyweb.ddhub.dto.ChannelDTO;
import org.energyweb.ddhub.dto.ConfigDTO;
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
    
    @ConfigProperty(name = "quarkus.http.limits.max-body-size")
    String fileMaxSize;
    
    @ConfigProperty(name = "NATS_MAX_CLIENT_ID")
    OptionalLong natsMaxClientId;

    
    @GET
    @Counted(name = "configuration_get_count", description = "", tags = {"ddhub=config"}, absolute = true)
    @Timed(name = "configuration_get_timed", description = "", tags = {"ddhub=config"}, unit = MetricUnits.MILLISECONDS, absolute = true)
    @Path("")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = ConfigDTO.class)))
    @Authenticated
    public Response configuration() throws IOException, JetStreamApiException, InterruptedException, ParseException {
        ConfigDTO config = new ConfigDTO();
        config.setMsgExpired(natsMaxAge);
        config.setMsgMaxSize(natsMaxSize);
        config.setFileMaxSize(convertKtoByte().longValue());
        config.setNatsMaxClientidSize(natsMaxClientId.orElse(ChannelDTO.DEFAULT_CLIENT_ID_SIZE));
        
        return Response.ok().entity(config).build();

    }


	private Float convertKtoByte() {
		final String[] METRIC_PREFIXES = new String[]{"", "k", "M", "G", "T"};

    	boolean isNegative = fileMaxSize.charAt(0) == '-';
        int length = fileMaxSize.length();

        String number = isNegative ? fileMaxSize.substring(1, length - 1) : fileMaxSize.substring(0, length - 1);
        String metricPrefix = Character.toString(fileMaxSize.charAt(length - 1));

        Number absoluteNumber = NumberUtils.createNumber(number);

        int index = 0;

        for (; index < METRIC_PREFIXES.length; index++) {
            if (METRIC_PREFIXES[index].equals(metricPrefix)) {
                break;
            }
        }

        Integer exponent = 3 * index;
        Double factor = Math.pow(10, exponent);
        factor *= isNegative ? -1 : 1;

        Float result = absoluteNumber.floatValue() * factor.longValue();
		return result;
	}
}