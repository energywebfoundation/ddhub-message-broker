package org.energyweb.ddhub;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.energyweb.ddhub.container.MongoDbResource;
import org.energyweb.ddhub.container.NatsResource;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTestResource(MongoDbResource.class)
@QuarkusTestResource(NatsResource.class)
public class ChannelTest {
	@Inject
	Logger logger;

	private String id;

	private String did = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D2";
	private String didUpload = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D4";

	@Test
	@Order(1)
	public void testCreateindex() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topics/createindex").andReturn();

		response.then()
				.statusCode(200)
				.body("returnCode", is("00"));

	}

	@Test
	@Order(2)
	public void testInitExtChannel() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("")
				.when()
				.post("/channel/initExtChannel").andReturn();

		response.then()
				.statusCode(200)
				.body("returnCode", is("00"));

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("")
				.when()
				.post("/channel/initExtChannel").andReturn();

		response.then()
				.statusCode(200)
				.body("returnCode", is("00"));
	}

	static String generateValidUserToken(String did) throws Exception {
		String privateKeyLocation = "/privatekey.pem";
		PrivateKey privateKey = readPrivateKey(privateKeyLocation);

		String[] roles = new String[] { "topiccreator.roles.messagebroker.apps.energyweb.iam.ewc",
				"topiccreator.roles.ddhub.apps.energyweb.iam.ewc", "user.roles.ddhub.apps.energyweb.iam.ewc","admin.roles.ddhub.apps.energyweb.iam.ewc" };
		return Jwt
				.claim("did", did)
				.claim("roles", new JSONArray(List.of(roles)))
				.sign(privateKey);
	}

	static PrivateKey readPrivateKey(final String pemResName) throws Exception {
		try (InputStream contentIS = JWT.class.getResourceAsStream(pemResName)) {
			byte[] tmp = new byte[4096];
			int length = contentIS.read(tmp);
			return decodePrivateKey(new String(tmp, 0, length, "UTF-8"));
		}
	}

	static PrivateKey decodePrivateKey(final String pemEncoded) throws Exception {
		byte[] encodedBytes = toEncodedBytes(pemEncoded);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(keySpec);
	}

	static byte[] toEncodedBytes(final String pemEncoded) {
		final String normalizedPem = removeBeginEnd(pemEncoded);
		return Base64.getDecoder().decode(normalizedPem);
	}

	static String removeBeginEnd(String pem) {
		pem = pem.replaceAll("-----BEGIN (.*)-----", "");
		pem = pem.replaceAll("-----END (.*)----", "");
		pem = pem.replaceAll("\r\n", "");
		pem = pem.replaceAll("\n", "");
		return pem.trim();
	}
}
