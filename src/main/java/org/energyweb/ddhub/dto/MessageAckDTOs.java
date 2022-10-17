package org.energyweb.ddhub.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageAckDTOs{

	public static final int MAX_FETCH_AMOUNT = 256;

    @Valid
    @NotNull
	private List<@NotEmpty String> messageIds;
    
    @Pattern(regexp = "^[a-zA-Z0-9\\-:.>*]+$", message = "Required Alphanumeric string")
	private String clientId = "mb-default";

    public int fetchAmount(long totalAckPending) {
    	int fetchAmount = messageIds.size();
		if(totalAckPending > 0 && totalAckPending > messageIds.size()) {
			fetchAmount = (int) totalAckPending;
		}
		return (fetchAmount > MessageAckDTOs.MAX_FETCH_AMOUNT)?MessageAckDTOs.MAX_FETCH_AMOUNT:fetchAmount;
	}
}
