package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseMessage {
    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String fqcn;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string")
    private String topicId;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String payload;

}
