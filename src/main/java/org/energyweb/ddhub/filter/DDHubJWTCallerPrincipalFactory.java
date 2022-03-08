package org.energyweb.ddhub.filter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;

@ApplicationScoped
@Alternative
@Priority(1)
public class DDHubJWTCallerPrincipalFactory extends JWTCallerPrincipalFactory {

    @Override
    public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
        try {
            // Token has already been verified, parse the token claims only
            if (token.split("\\.").length != 3) {
                throw new ParseException("");
            }
            String json = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
            return new DefaultJWTCallerPrincipal(JwtClaims.parse(json));
        } catch (InvalidJwtException ex) {
            throw new ParseException(ex.getMessage());
        }
    }
}