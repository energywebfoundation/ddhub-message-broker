package org.energyweb.ddhub;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
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
public class DDhubTest {
	@Inject
	Logger logger;

	static private String id, id2;

	private String did = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D2";
	private String didUpload = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D4";
	private String didTest = "did:ethr:0xfd6b809B81cAEbc3EAB0d33f0211E5934621b2D6";

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

	@Test
	@Order(3)
	public void testSendMessage() throws Exception {
		HashMap createTopic = new HashMap<>();
		createTopic.put("name", "createTopic01");
		createTopic.put("schemaType", "JSD7");
		createTopic.put("schema",
				"{\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"properties\": {\n    \"data\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"initiatingMessageId\": {\n          \"type\": \"string\"\n        },\n        \"initiatingTransactionId\": {\n          \"type\": [\n                      \"string\",\n                      \"null\"\n                  ]\n        },\n        \"systemProcessedDttm\": {\n          \"type\": \"string\"\n        }\n      },\n      \"required\": [\n        \"initiatingMessageId\",\n        \"initiatingTransactionId\",\n        \"systemProcessedDttm\"\n      ]\n    },\n    \"dispatchAcknowledgements\": {\n      \"type\": \"array\",\n      \"items\": [\n        {\n          \"type\": \"object\",\n          \"properties\": {\n            \"dispatchId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementDateTime\": {\n              \"type\": \"string\"\n            },\n            \"facilityId\": {\n              \"type\": \"string\"\n            },\n            \"nmis\": {\n              \"type\": \"array\",\n              \"items\": [\n                {\n                  \"type\": \"string\"\n                }\n              ]\n            }\n          },\n          \"required\": [\n            \"dispatchId\",\n            \"acknowledgementId\",\n            \"acknowledgementDateTime\",\n            \"facilityId\",\n            \"nmis\"\n          ]\n        }\n      ]\n    }\n  },\n  \"required\": [\n    \"data\",\n    \"dispatchAcknowledgements\"\n  ]\n}");
		createTopic.put("version", "1.0.0");
		createTopic.put("owner", "ddhub.apps.energyweb.iam.ewc");
		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(createTopic))
				.when()
				.post("/topics").andReturn();

		id2 = response.then()
				.statusCode(200)
				.extract().body().jsonPath().getString("id");
		id = id2;

		HashMap sendmsg = new HashMap<>();
		sendmsg.put("fqcns", Arrays.asList(did, "diderror"));
		sendmsg.put("transactionId", "testid");
		sendmsg.put("clientGatewayMessageId", "14645780-099d-41a6-a8e1-64caa59ad290");
		sendmsg.put("payload", "{}");
		sendmsg.put("topicId", id);
		sendmsg.put("topicVersion", "1.0.0");
		sendmsg.put("signature", "signature");
		sendmsg.put("payloadEncryption", "false");
		sendmsg.put("initiatingMessageId", "test1");
		sendmsg.put("topicRestrictions", "topictest1");

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(sendmsg))
				.when()
				.post("/messages").andReturn();

		String msgId = response.then()
				.statusCode(200).extract().body().jsonPath().getString("messageId");

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("{\n  \"topicId\": [\n    \"" + id + "\"\n  ],\"fqcnTopicList\": [\n    \"" + id + "\"\n  ],\n  \"senderId\": [\n    \"" + did + "\"\n  ]\n}")
				.when()
				.post("/messages/search").andReturn();

		response.then()
				.statusCode(200)
				.body("size()", equalTo(1))
				.body("[0].id", equalTo(msgId));

	}

	@Test
	@Order(4)
	public void testUploadDownload() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("")
				.when()
				.post("/channel/initExtChannel").andReturn();

		response.then()
				.statusCode(200)
				.body("returnCode", is("00"));

		HashMap createTopic = new HashMap<>();
		createTopic.put("name", "createTopic02");
		createTopic.put("schemaType", "JSD7");
		createTopic.put("schema",
				"{\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"properties\": {\n    \"data\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"initiatingMessageId\": {\n          \"type\": \"string\"\n        },\n        \"initiatingTransactionId\": {\n          \"type\": [\n                      \"string\",\n                      \"null\"\n                  ]\n        },\n        \"systemProcessedDttm\": {\n          \"type\": \"string\"\n        }\n      },\n      \"required\": [\n        \"initiatingMessageId\",\n        \"initiatingTransactionId\",\n        \"systemProcessedDttm\"\n      ]\n    },\n    \"dispatchAcknowledgements\": {\n      \"type\": \"array\",\n      \"items\": [\n        {\n          \"type\": \"object\",\n          \"properties\": {\n            \"dispatchId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementDateTime\": {\n              \"type\": \"string\"\n            },\n            \"facilityId\": {\n              \"type\": \"string\"\n            },\n            \"nmis\": {\n              \"type\": \"array\",\n              \"items\": [\n                {\n                  \"type\": \"string\"\n                }\n              ]\n            }\n          },\n          \"required\": [\n            \"dispatchId\",\n            \"acknowledgementId\",\n            \"acknowledgementDateTime\",\n            \"facilityId\",\n            \"nmis\"\n          ]\n        }\n      ]\n    }\n  },\n  \"required\": [\n    \"data\",\n    \"dispatchAcknowledgements\"\n  ]\n}");
		createTopic.put("version", "1.0.0");
		createTopic.put("owner", "ddhub.apps.energyweb.iam.ewc");

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(createTopic))
				.when()
				.post("/topics").andReturn();

		id = response.then()
				.statusCode(200)
				.extract().body().jsonPath().getString("id");

//		String filename = "testUploadDownload.csv";
//		response = given().auth()
//				.oauth2(generateValidUserToken(didUpload))
//				.header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA)
//				.multiPart("fqcns", didUpload)
//				.multiPart("file", new File(DDhubTest.class.getResource("/sample.txt").getFile()))
//				.multiPart("fileName", filename)
//				// .multiPart("transactionId", fruit)
//				.multiPart("signature", "signature")
//				.multiPart("topicId", id)
//				.multiPart("topicVersion", "1.0.0")
//				.multiPart("clientGatewayMessageId", "test")
//				.multiPart("payloadEncryption", "false")
//				.when()
//				.post("/messages/upload").andReturn();
//
//		response.then()
//				.statusCode(200);
//
//		response = given().auth()
//				.oauth2(generateValidUserToken(didUpload))
//				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
//				.body("{\n  \"topicId\": [\n    \"" + id + "\"\n  ],\n  \"senderId\": [\n    \"" + didUpload
//						+ "\"\n  ]\n}")
//				.when()
//				.post("/messages/search").andReturn();
//
//		HashMap payload = response.then().statusCode(200).extract().body().jsonPath().getList(".", HashMap.class)
//				.get(0);
//		JSONObject jsonObject = (JSONObject) new JSONParser().parse(payload.get("payload").toString());
//
//		response = given().auth()
//				.oauth2(generateValidUserToken(didUpload))
//				.when()
//				.get("/messages/download?fileId=" + jsonObject.get("fileId")).andReturn();
//
//		response.then()
//				.statusCode(200);
//
//		Assertions.assertTrue(response.getHeader("Content-Disposition").contains(filename));

	}

//	@Test
//	@Order(5)
	public void testGetMessage() throws Exception {
		HashMap createTopic = new HashMap<>();
		createTopic.put("name", "createTopic03");
		createTopic.put("schemaType", "JSD7");
		createTopic.put("schema",
				"{\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"properties\": {\n    \"data\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"initiatingMessageId\": {\n          \"type\": \"string\"\n        },\n        \"initiatingTransactionId\": {\n          \"type\": [\n                      \"string\",\n                      \"null\"\n                  ]\n        },\n        \"systemProcessedDttm\": {\n          \"type\": \"string\"\n        }\n      },\n      \"required\": [\n        \"initiatingMessageId\",\n        \"initiatingTransactionId\",\n        \"systemProcessedDttm\"\n      ]\n    },\n    \"dispatchAcknowledgements\": {\n      \"type\": \"array\",\n      \"items\": [\n        {\n          \"type\": \"object\",\n          \"properties\": {\n            \"dispatchId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementId\": {\n              \"type\": \"string\"\n            },\n            \"acknowledgementDateTime\": {\n              \"type\": \"string\"\n            },\n            \"facilityId\": {\n              \"type\": \"string\"\n            },\n            \"nmis\": {\n              \"type\": \"array\",\n              \"items\": [\n                {\n                  \"type\": \"string\"\n                }\n              ]\n            }\n          },\n          \"required\": [\n            \"dispatchId\",\n            \"acknowledgementId\",\n            \"acknowledgementDateTime\",\n            \"facilityId\",\n            \"nmis\"\n          ]\n        }\n      ]\n    }\n  },\n  \"required\": [\n    \"data\",\n    \"dispatchAcknowledgements\"\n  ]\n}");
		createTopic.put("version", "1.0.0");
		createTopic.put("owner", "ddhub.apps.energyweb.iam.ewc");

		Response response = given().auth()
				.oauth2(generateValidUserToken(didTest))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(createTopic))
				.when()
				.post("/topics").andReturn();

		String id3 = response.then()
				.statusCode(200)
				.extract().body().jsonPath().getString("id");

		HashMap sendmsg = new HashMap<>();
		sendmsg.put("fqcns", Arrays.asList(didUpload));
		sendmsg.put("transactionId", "testid");
		sendmsg.put("clientGatewayMessageId", "testid");
		sendmsg.put("payload", "payload");
		sendmsg.put("topicId", id3);
		sendmsg.put("topicVersion", "1.0.0");
		sendmsg.put("payloadEncryption", "false");
		sendmsg.put("signature", "signature");

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(sendmsg))
				.when()
				.post("/messages").andReturn();

		response.then()
				.statusCode(200);

		HashMap topic = new HashMap();
		topic.put("topicId", Arrays.asList(id, id2));
		topic.put("senderId", Arrays.asList(didUpload, did));
		topic.put("clientId", id);

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/messages/search").andReturn();

		response.then()
				.statusCode(200)
				.body("size()", equalTo(1));

		topic = new HashMap();
		topic.put("topicId", Arrays.asList(id, id2));
		topic.put("senderId", Arrays.asList(didUpload, did));
		topic.put("clientId", id + "1");
		topic.put("amount", 10);

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/messages/search").andReturn();

		response.then()
				.statusCode(200)
				.body("size()", equalTo(1));

		topic = new HashMap();
		topic.put("topicId", Arrays.asList(id, id2));
		topic.put("senderId", Arrays.asList(didUpload, did));
		topic.put("clientId", id + "2");
		topic.put("amount", 10);
		topic.put("from", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(Date
				.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault())
						.toInstant())));

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/messages/search").andReturn();
		logger.info(response.then().extract().asString());
		response.then()
				.statusCode(200)
				.body("size()", equalTo(1));

		topic = new HashMap();
		topic.put("topicId", Arrays.asList(id, id2));
		topic.put("senderId", Arrays.asList(didUpload, did));
		topic.put("clientId", id + "3");
		topic.put("amount", 10);
		topic.put("from", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(Date
				.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault())
						.toInstant())));

		response = given().auth()
				.oauth2(generateValidUserToken(didUpload))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(topic))
				.when()
				.post("/messages/search").andReturn();

		response.then()
				.statusCode(200)
				.body("size()", equalTo(0));

	}

	@Test
	public void testRole() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/roles/check?did=" + did + "&roles=topiccreator.roles.messagebroker.apps.energyweb.iam.ewc")
				.andReturn();

		response.then()
				.statusCode(200)
				.body("hasRole", equalTo(true));

		response = given().auth()
				.oauth2(generateValidUserToken(did))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.when()
				.get("/roles/list?roles=topiccreator.roles.messagebroker.apps.energyweb.iam.ewc").andReturn();

		response.then()
				.statusCode(200)
				.body("dids.size()", equalTo(2));
	}

	@Test
	public void testInternalMessage() throws Exception {

		String payload = "test data";
		HashMap msg = new HashMap();
		msg.put("fqcn", didTest);
		msg.put("clientGatewayMessageId", didTest);
		msg.put("payload", payload);

		Response response = given().auth()
				.oauth2(generateValidUserToken(didTest))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body("")
				.when()
				.post("/channel/initExtChannel").andReturn();

		response = given().auth()
				.oauth2(generateValidUserToken(didTest))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(msg))
				.when()
				.post("/messages/internal").andReturn();

		response.then()
				.statusCode(200)
				.body("messageId", notNullValue());

		msg = new HashMap();
		msg.put("senderId", Arrays.asList(didTest));
		msg.put("clientId", id + "1");
		msg.put("amount", 10);
		response = given().auth()
				.oauth2(generateValidUserToken(didTest))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.body(JsonbBuilder.create().toJson(msg))
				.when()
				.post("/messages/internal/search").andReturn();

		logger.info(response.then().statusCode(200).extract().asString());
		response.then()
				.statusCode(200)
				.body("[0].payload", equalTo(payload));
	}

	@Test
	public void testTokenGenerator() throws Exception {

		Response response = given().auth()
				.oauth2(generateValidUserToken(did))
				.when()
				.get("/token/generator?DID=sss&role=strings").andReturn();

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
