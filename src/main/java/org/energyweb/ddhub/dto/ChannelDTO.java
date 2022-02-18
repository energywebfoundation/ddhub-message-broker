package org.energyweb.ddhub.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChannelDTO extends DDHub {

    @NotNull
    @Max(value = 86400000)
    private Long maxMsgAge;
    @DefaultValue("1048576")
    @Max(value = 8388608)
    @NotNull
    private Long maxMsgSize;

    @Valid
    private Set<@Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") String> topicIds;
    
    @Valid
    @NotNull
    @NotEmpty
    private Set<@NotNull @NotEmpty String> admins;
    
    @Valid
    @NotNull
    @NotEmpty
    private Set<@NotNull @NotEmpty String> pubsub;

    @NotNull
    @DefaultValue("true")
    private Boolean encryption;
    
    @JsonIgnore
    private String owner;

    @JsonIgnore
    private String updateBy;

    @JsonIgnore
    public List<String> findArraySubjectName() {
        if (Optional.ofNullable(topicIds).isEmpty())
            return null;

        List<String> topics = new ArrayList<String>();
        topicIds.forEach(topic -> {

            topics.add(streamName().concat(".").concat(topic));
        });
        return topics;
    }
}
