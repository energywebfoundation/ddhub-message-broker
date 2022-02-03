package org.energyweb.ddhub.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChannelDTO extends DDHub {

	
	@NotNull
	@Max(value = 1440)
	@JsonProperty("maxMsgAge")
	private Long maxMsgAge;
	@Max(value = 8388608)
	@NotNull
	@JsonProperty("maxMsgSize")
	private Long maxMsgSize;

   
}
