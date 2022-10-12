package org.energyweb.ddhub.dto;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientDTO {

	@Valid
    @NotNull
    @NotEmpty
    private Set<@NotNull @NotEmpty String> clientIds;
}
