package org.energyweb.ddhub.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChannelDTO {

	@NotNull
	@NotEmpty
	@JsonProperty("fqcn")
    private String fqcn;
	@NotEmpty
	@NotNull
	@JsonProperty("topic")
	private String topic;
	@NotNull
	@Max(value = 1440)
	@JsonProperty("maxMsgAge")
	private Long maxMsgAge;
	@Max(value = 8388608)
	@NotNull
	@JsonProperty("maxMsgSize")
	private Long maxMsgSize;

    @JsonIgnore
    public String getStreamName() {
        String[] streamName = fqcn.split(Pattern.quote("."));
        Collections.reverse(Arrays.asList(streamName));
        return String.join("_", streamName);
    }

    @JsonIgnore
    public String getSubjectName() {
        return getStreamName().concat(".").concat(topic);
    }
}
