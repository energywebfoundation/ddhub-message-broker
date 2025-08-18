package org.energyweb.ddhub.dto;

import java.util.Set;

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
public class ClientDTO {

    @Valid
    @NotNull
    @NotEmpty
    private Set<@NotNull @NotEmpty @Pattern(regexp = "^(?:&(#\\d+;|#x[0-9A-Fa-f]+;|lt;|gt;|quot;|amp;)|[^&<>\"'/\\\\\\-\\.\\u0008\\u000C\\u000A\\u000D\\u0009])*$",message = "Invalid characters detected.") String> clientIds;
}
