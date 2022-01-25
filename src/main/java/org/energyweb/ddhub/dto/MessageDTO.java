package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageDTO {
    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String fqcn;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String topic;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String correlationId;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String payload;
}
