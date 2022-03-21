package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InternalMessageDTO extends DDHub {
   

    @Size(max = 200, message = "The maximum length is 200 characters")
    private String transactionId;

    @NotNull
    @NotEmpty
    @Size(max = 8192, message = "The maximum length is 8192 characters")
    private String payload;
    
    @JsonIgnore
    private String id;
    
    @JsonIgnore
    private String senderDid;

    @JsonIgnore
    private String geatewayDid;
    
    @JsonIgnore
	private long timestampNanos;
    
    
    
}
