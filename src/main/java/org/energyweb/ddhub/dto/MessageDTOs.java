package org.energyweb.ddhub.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageDTOs extends MessageDTO {
    @Valid
    @NotNull
	private List<@NotEmpty 	@Size(max = 200, message = "The maximum length is 200 characters") String> fqcns;

}
