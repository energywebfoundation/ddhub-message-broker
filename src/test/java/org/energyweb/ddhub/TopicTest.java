package org.energyweb.ddhub;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.bind.JsonbBuilder;
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
public class TopicTest {
	@Inject
	Logger logger;

	static private String id;

	private String did = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D2";
	private String didUpload = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D4";

	@Test
	@Order(1)
	public void testCreateindex() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic/createindex").andReturn();

		response.then()
				.statusCode(200)
				.body("returnCode", is("00"));

	}

	@Test
	@Order(2)
	public void testTopicCreate() throws Exception {
		HashMap topic = new HashMap();
		topic.put("name", "topic1");
		topic.put("schemaType", "JSD7");
		topic.put("schema",
				"{\n  \"topicId\": \"6225cdd78a3564730dc8fb09,\n  \"senderId\": [\n    \"6225cdd78a3564730dc8fb09\"\n  ]\n}");
		topic.put("version", "1.0.0");
		topic.put("owner", "test.apps.energyweb.iam.ewc");
		topic.put("tags", Arrays.asList("test1").toArray());

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/topic").andReturn();

		response.then()
				.statusCode(200)
				.body("id", notNullValue());

		topic = new HashMap();
		topic.put("name", "topic2");
		topic.put("schemaType", "JSD7");
		topic.put("schema",
				"{\n  \"topicId\": \"6225cdd78a3564730dc8fb09,\n  \"senderId\": [\n    \"6225cdd78a3564730dc8fb09\"\n  ]\n}");
		topic.put("version", "1.0.0");
		topic.put("owner", "test.apps.energyweb.iam.ewc");
		topic.put("tags", Arrays.asList("test2").toArray());

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/topic").andReturn();

		response.then()
				.statusCode(200)
				.body("id", notNullValue());

		id = response.then().statusCode(200).extract().body().jsonPath().get("id");

		topic = new HashMap();
		topic.put("id", id);
		topic.put("name", "topic2");
		topic.put("schemaType", "JSD7");
		topic.put("schema",
				"{\n  \"topicId\": \"6225cdd78a3564730dc8fb09,\n  \"senderId\": [\n    \"6225cdd78a3564730dc8fb09\"\n  ]\n}");
		topic.put("version", "1.0.1");
		topic.put("owner", "test.apps.energyweb.iam.ewc");
		topic.put("tags", Arrays.asList("test2").toArray());

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.patch("/topic").andReturn();

		response.then()
				.statusCode(200)
				.body("returnCode", is("00"));

		topic = new HashMap();
		topic.put("id", id);
		topic.put("name", "topic3");
		topic.put("schemaType", "JSD7");
		topic.put("schema",
				"{\n  \"topicId\": \"6225cdd78a3564730dc8fb09,\n  \"senderId\": [\n    \"6225cdd78a3564730dc8fb09\"\n  ]\n}");
		topic.put("version", "1.0.1");
		topic.put("owner", "test.apps.energyweb.iam.ewc");
		topic.put("tags", Arrays.asList("test2").toArray());

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.patch("/topic").andReturn();

		response.then()
				.statusCode(400);

	}

	@Test
	@Order(5)
	public void testTopicSearch() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic/search?keyword=topic").andReturn();

		response.then()
				.statusCode(200)
				.body("count", is(2));
	}

	@Test
	@Order(3)
	public void testTopicVersion() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic/{id}/version", id).andReturn();
		response.then()
				.statusCode(200)
				.body("count", is(2));

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic/{id}/version/{versionNumber}", id, "1.0.1").andReturn();
		response.then()
				.statusCode(200)
				.body("version", is("1.0.1"));

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic/{id}/version/{versionNumber}", id, "1.0.2").andReturn();
		response.then()
				.statusCode(400)
				.body("returnMessage", containsString("version not exists"));

	}

	@Test
	@Order(4)
	public void testTopicCount() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic/count?owner=test.apps.energyweb.iam.ewc").andReturn();

		logger.info(response.then().extract().asString());
		response.then()
				.statusCode(200)
				.body("'test.apps.energyweb.iam.ewc'", is(2));
	}

	@Test
	@Order(6)
	public void testTopicQuery() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/topic?limit={limit}&name={name}&owner={owner}&page={page}", 0, "", "test.apps.energyweb.iam.ewc",
						1)
				.andReturn();

		response.then()
				.statusCode(200)
				.body("count", is(2));
	}

	@Test
	@Order(10)
	public void testTopicDelete() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.delete("/topic/{id}", id).andReturn();

		logger.info(response.then().extract().asString());
		response.then()
				.statusCode(200);
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
