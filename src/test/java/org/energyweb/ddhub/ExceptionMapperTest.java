package org.energyweb.ddhub;

import static io.restassured.RestAssured.given;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.energyweb.ddhub.container.MongoDbResource;
import org.energyweb.ddhub.container.NatsResource;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.junit.jupiter.api.MethodOrderer;
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
public class ExceptionMapperTest {
	@Inject
	Logger logger;

	private String id;

	private String did = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D2";
	private String didUpload = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D4";

	@Test
	public void testValidation() throws JsonbException, Exception {
		HashMap topic = new HashMap();

		Response response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/messages/search").andReturn();

		response = given().auth()
				.oauth2("eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkaWQ6ZXRocjoweDNDZTNCNjA0MjdiNEJmMENlMzY2ZDk5NjNCZUM1ZWYzQ0JEMDZhZDUiLCJjbGFpbURhdGEiOnsiYmxvY2tOdW1iZXIiOjk5OTk5OTk5OTk5OX19.MHgzNmFmZjY2YTViNmQ5ZTNhODE2NDYzOTUzMzcxNGRmZWM0YjE4MWQxYTk1YTBkY2Y0OTM3NTRlYjhlZGFlYzg3NjlkOGMyOTdhNDBiMTc4NGNmMmFmM2FlM2I1MTBkOTAwYTI3MWYwY2NmMDQ1ODI3NGYwOGY1MjQ2YWZmMjgwYjFi")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("")
				.when()
				.post("/messages/search").andReturn();

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("{\n  \"fqcn\": \"" + didUpload
						+ "\",\n  \"transactionId\": \"string\",\n  \"payload\": \"string\",\n  \"topicId\": \"62170370fff91062c17df720"
						+ "\",\n  \"topicVersion\": \"1.0.0\",\n  \"signature\": \"string\"\n},")
				.when()
				.post("/messages").andReturn();

		HashMap msg = new HashMap();
		msg.put("fqcn", didUpload + 1);
		msg.put("transactionId", didUpload);
		msg.put("payload", "ddd");

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(msg))
				.when()
				.post("/messages/internal").andReturn();

		HashMap createTopic = new HashMap<>();
		createTopic.put("name", "createTopic04");
		createTopic.put("schemaType", "JSD7");
		createTopic.put("schema", "{\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"properties\": {\n    \"data\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"initiatingMessageId\": {\n          \"type\": \"string\"\n        },\n        \"initiatingTransactionId\": {\n          \"type\": [\n                      \"string\",\n                      \"null\"\n                  ]\n        },\n        \"systemProcessedDttm\": {\n          \"type\": \"string\"\n        }\n      },\n      \"required\": [\n        \"initiatingMessageId\",\n        \"initiatingTransactionId\",\n        \"systemProcessedDttm\"\n      ]\n    },\n    \"dispatchAcknowledgements\": {\n      \"type\": \"array\",\n      \"items\": [\n        {\n          \"type\": \"object\",\n          \"properties\": {\n            \"dispatchId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementDateTime\": {\n              \"type\": \"string\"\n            },\n            \"facilityId\": {\n              \"type\": \"string\"\n            },\n            \"nmis\": {\n              \"type\": \"array\",\n              \"items\": [\n                {\n                  \"type\": \"string\"\n                }\n              ]\n            }\n          },\n          \"required\": [\n            \"dispatchId\",\n            \"acknowledgementId\",\n            \"acknowledgementDateTime\",\n            \"facilityId\",\n            \"nmis\"\n          ]\n        }\n      ]\n    }\n  },\n  \"required\": [\n    \"data\",\n    \"dispatchAcknowledgements\"\n  ]\n}");
		createTopic.put("version", "1.0.0");
		createTopic.put("owner", "ddhub.apps.energyweb.iam.ewc");
		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(createTopic))
				.when()
				.post("/topics").andReturn();

		String id3 = response.then()
				.statusCode(200)
				.extract().body().jsonPath().getString("id");

		topic = new HashMap();
		topic.put("topicId", Arrays.asList(id3));
		topic.put("senderId", Arrays.asList(didUpload, did));
		topic.put("clientId", id + "1");
		topic.put("amount", -1);

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/messages/search").andReturn();

		response = given().auth()
				.oauth2(generateValidUserToken(">as.as.as"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("")
				.when()
				.post("/channel/initExtChannel").andReturn();

	}

	static String generateValidUserToken(String did) throws Exception {
		String privateKeyLocation = "/privatekey.pem";
		PrivateKey privateKey = readPrivateKey(privateKeyLocation);

		String[] roles = new String[] { "topiccreator.roles.messagebroker.apps.energyweb.iam.ewc",
				"topiccreator.roles.ddhub.apps.energyweb.iam.ewc", "user.roles.ddhub.apps.energyweb.iam.ewc" };
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
