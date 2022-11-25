package org.energyweb.ddhub.dto;

import java.time.LocalDateTime;
import java.util.List;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageAckDTOs{

	public static final int MAX_FETCH_AMOUNT = 256;
	
	@Size(min = 1, max = 255, message = "The length between 1-255 characters")
	private String anonymousRecipient;

    @Valid
    @NotNull
	private List<@NotEmpty String> messageIds;
    
    @Pattern(regexp = "^[a-zA-Z0-9\\-:.>*]+$", message = "Required Alphanumeric string")
    @Size(max=247, message = "The maximum length is 247 characters")
	private String clientId = "mb-default";
    
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private LocalDateTime from;

    public int fetchAmount(long totalAckPending) {
    	int fetchAmount = messageIds.size();
		if(totalAckPending > 0 && totalAckPending > messageIds.size()) {
			fetchAmount = (int) totalAckPending;
		}
		return (fetchAmount > MessageAckDTOs.MAX_FETCH_AMOUNT)?MessageAckDTOs.MAX_FETCH_AMOUNT:fetchAmount;
	}
}
