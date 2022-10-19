package org.energyweb.ddhub.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageAckDTO{

	@Valid
    @NotNull
	private List<@NotEmpty String> acked;
	@Valid
    @NotNull
	private List<@NotEmpty String> notFound;
    
}
