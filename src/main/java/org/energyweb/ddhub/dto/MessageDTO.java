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
    public static final long REQUEST_TIMEOUT = 1;
    public static final long CONNECTION_TIMEOUT = 5;

    @Size(max = 200, message = "The maximum length is 200 characters")
    private String transactionId;

    @Size(max = 200, message = "The maximum length is 200 characters")
    private String initiatingMessageId;

    @Size(max = 200, message = "The maximum length is 200 characters")
    private String initiatingTransactionId;

    @Size(max = 500, message = "The maximum length is 500 characters")
    private String topicRestrictions;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    @Pattern(regexp = "^[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-4[0-9a-fA-F]{3}\\-[89abAB][0-9a-fA-F]{3}\\-[0-9a-fA-F]{12}$", message = "Invalid UUID v4 format")
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
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z-.]+)?(\\+[0-9A-Za-z-.]+)?$", message = "Required Semantic Versions")
    private String topicVersion;

    @NotNull
    @NotEmpty
    @Pattern(regexp = "^[^&<>\"'/\\\\\\-\\.\\r\\n\\t]*$", message = "Invalid characters detected.")
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
        return UUID.nameUUIDFromBytes(
                (getTransactionId() + getSenderDid() + getFqcn() + getTopicId()).getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}
