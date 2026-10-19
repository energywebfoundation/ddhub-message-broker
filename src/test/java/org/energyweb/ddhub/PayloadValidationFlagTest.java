package org.energyweb.ddhub;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.energyweb.ddhub.dto.MessageDTOs;
import org.energyweb.ddhub.dto.TopicDTO;
import org.energyweb.ddhub.exception.InvalidPayloadException;
import org.junit.jupiter.api.Test;

public class PayloadValidationFlagTest {

	private static final String UUID = "14645780-099d-41a6-a8e1-64caa59ad290";
	// What ddhub-client-gateway sends: plain string fields are already Trustwave-encoded
	private static final String ENCODED_UUID = "14645780&#x2D;099d&#x2D;41a6&#x2D;a8e1&#x2D;64caa59ad290";
	private static final String PLAIN_SCHEMA = "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}";
	private static final String UUID_FORMAT_SCHEMA = "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"uuid\"}}}";

	private static String processPayload(boolean validationEnabled, String schema, String payload, boolean payloadEncryption) {
		Message message = new Message();
		message.payloadValidationEnabled = validationEnabled;

		TopicDTO topic = new TopicDTO();
		topic.setSchemaType("JSD7");
		topic.setSchema(schema);

		MessageDTOs messageDTOs = new MessageDTOs();
		messageDTOs.setPayload(payload);
		messageDTOs.setPayloadEncryption(payloadEncryption);

		message.processPayload(messageDTOs, topic);
		return messageDTOs.getPayload();
	}

	private static String processPayload(boolean validationEnabled, String payload) {
		return processPayload(validationEnabled, PLAIN_SCHEMA, payload, false);
	}

	private static String idPayload(String id) {
		return "{\"id\":\"" + id + "\"}";
	}

	// PAYLOAD_VALIDATION_ENABLED=false

	@Test
	public void testDisabledUuidIsUnchanged() {
		assertEquals(idPayload(UUID), processPayload(false, idPayload(UUID)));
	}

	@Test
	public void testDisabledUppercaseUuidIsUnchanged() {
		String upper = UUID.toUpperCase();
		assertEquals(idPayload(upper), processPayload(false, idPayload(upper)));
	}

	@Test
	public void testDisabledEncodedUuidIsUnchanged() {
		assertEquals(idPayload(ENCODED_UUID), processPayload(false, idPayload(ENCODED_UUID)));
	}

	@Test
	public void testDisabledInvalidJsonIsAccepted() {
		assertEquals("not json", processPayload(false, "not json"));
	}

	// PAYLOAD_VALIDATION_ENABLED=true

	@Test
	public void testEnabledEncodedUuidIsAcceptedUnchanged() {
		assertEquals(idPayload(ENCODED_UUID), processPayload(true, idPayload(ENCODED_UUID)));
	}

	@Test
	public void testEnabledEncodedUppercaseUuidIsAcceptedUnchanged() {
		String upper = "14645780&#x2D;099D&#x2D;41A6&#x2D;A8E1&#x2D;64CAA59AD290";
		assertEquals(idPayload(upper), processPayload(true, idPayload(upper)));
	}

	@Test
	public void testEnabledUuidWithUuidFormatIsAcceptedUnchanged() {
		assertEquals(idPayload(UUID), processPayload(true, UUID_FORMAT_SCHEMA, idPayload(UUID), false));
	}

	@Test
	public void testEnabledRawUuidInPlainStringIsRejected() {
		assertThrows(InvalidPayloadException.class, () -> processPayload(true, idPayload(UUID)));
	}

	@Test
	public void testEnabledEncodedNamedEntitiesAreAcceptedUnchanged() {
		String payload = idPayload("Jos&eacute; caf&eacute; &copy; &euro; non&#x2D;breaking&nbsp;space");
		assertEquals(payload, processPayload(true, payload));
	}

	@Test
	public void testEnabledEncodedMarkupIsAcceptedUnchanged() {
		String payload = idPayload("&lt;script&gt;alert(1)&lt;&#x2F;script&gt;");
		assertEquals(payload, processPayload(true, payload));
	}

	@Test
	public void testEnabledRawMarkupIsRejected() {
		assertThrows(InvalidPayloadException.class, () -> processPayload(true, idPayload("<script>alert(1)</script>")));
	}

	@Test
	public void testEnabledMalformedEntityIsRejected() {
		assertThrows(InvalidPayloadException.class, () -> processPayload(true, idPayload("a &<b>; c")));
	}

	@Test
	public void testEnabledCompactJsonIsNotReformatted() {
		String payload = "{\"id\":\"abc\"}";
		assertEquals(payload, processPayload(true, payload));
	}

	@Test
	public void testEnabledSchemaViolationIsRejected() {
		assertThrows(InvalidPayloadException.class, () -> processPayload(true, "{\"id\":123}"));
	}

	@Test
	public void testEnabledInvalidJsonIsRejected() {
		assertThrows(InvalidPayloadException.class, () -> processPayload(true, "not json"));
	}

	@Test
	public void testEnabledEncryptedPayloadIsUnchanged() {
		assertDoesNotThrow(() -> processPayload(true, PLAIN_SCHEMA, "not json", true));
		assertEquals(idPayload(UUID), processPayload(true, PLAIN_SCHEMA, idPayload(UUID), true));
	}
}
