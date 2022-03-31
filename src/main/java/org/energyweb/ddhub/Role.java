package org.energyweb.ddhub;

import java.util.HashMap;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.energyweb.ddhub.repository.RoleOwnerRepository;
import org.jboss.logging.Logger;

import io.quarkus.security.Authenticated;

@Path("/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(value = @Tag(name = "Role", description = "All the methods"))
@SecurityRequirement(name = "AuthServer")
@RequestScoped
public class Role {

    public enum TYPE {
        ALL,
        ANY
    }

    @Inject
    Logger logger;

    @Inject
    RoleOwnerRepository ownerRepository;

    @Inject
    @Claim(value = "did")
    String DID;

    @Inject
    @Claim(value = "roles")
    String verifiedRoles;

    @GET
    @Path("check")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = HashMap.class)))
    @Authenticated
    public Response hasRoles(@NotEmpty @NotEmpty @QueryParam("did") String did,
            @NotEmpty @NotEmpty @QueryParam("roles") String... roles) {
        HashMap<String, Boolean> ret = new HashMap<>();
        ret.put("hasRole", ownerRepository.hasRoles(did, roles));
        return Response.ok().entity(ret).build();

    }

    @GET
    @Path("list")
    @APIResponse(description = "", content = @Content(schema = @Schema(implementation = HashMap.class)))
    @Authenticated
    public Response getDidsByRoles(@DefaultValue("ANY") @QueryParam("searchType") TYPE searchType,
            @NotEmpty @NotEmpty @QueryParam("roles") String... roles) {
    	 HashMap<String, List<String>> ret = new HashMap<>();
         ret.put("dids", ownerRepository.queryDidsByRoles(searchType, roles));
        return Response.ok().entity(ret).build();

    }
}