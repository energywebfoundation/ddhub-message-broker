package org.energyweb.ddhub.dto;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MessageDTO extends DDHub {
	public static final int MAX_RECONNECTS = 3;
	public static final long REQUEST_TIMEOUT = 2;
	public static final long CONNECTION_TIMEOUT = 5;

	@Size(max = 200, message = "The maximum length is 200 characters")
    private String transactionId;
    
    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String clientGatewayMessageId;
    
    @NotNull
    private boolean payloadEncryption;

    @NotNull
    @NotEmpty
    private String payload;
    
    @NotNull
    @NotEmpty
    @Size(max = 1024, message = "The maximum length is 1024 characters")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string")
    private String topicId;
    
    @NotNull
    @NotEmpty
    @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions")
	private String topicVersion;
    
    @NotNull
    @NotEmpty
    private String signature;

    @JsonIgnore
    @JsonbProperty(value = "messageId")
    private String id;

    @JsonIgnore
    private String senderDid;

    @JsonIgnore
    private String geatewayDid;
    
    @JsonIgnore
	private long timestampNanos;
    
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private boolean fromUpload = false;
    
    
    public boolean getIsFile() {
    	return fromUpload;

    }
    
    public void setIsFile(boolean isFile) {
    	fromUpload = isFile;
    }
    
    @JsonIgnore
	public String subjectName() {
		if (StringUtils.isBlank(topicId))
			return null;
		return super.streamName().concat(".").concat(topicId);
	}
    
    @JsonIgnore
	public String subjectAll() {
		return super.streamName().concat(".").concat("*");
	}

	public String storageName() {
		return super.streamName() + "/" + subjectName() + "/";
	}

	public String createNatsTransactionId() {
		return UUID.nameUUIDFromBytes((getTransactionId() + getSenderDid() + getFqcn() + getTopicId()).getBytes(StandardCharsets.UTF_8)).toString();
	}
}
