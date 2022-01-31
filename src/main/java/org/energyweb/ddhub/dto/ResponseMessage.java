package org.energyweb.ddhub.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private String topic;

    @NotNull
    @NotEmpty
    @Size(max = 200, message = "The maximum length is 200 characters")
    private String payload;

}
