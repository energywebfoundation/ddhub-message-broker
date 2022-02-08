package org.energyweb.ddhub.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "channel")
@Data
@NoArgsConstructor
public class ChannelDTO extends DDHub {

    @NotNull
    @Max(value = 1440)
    @JsonProperty("maxMsgAge")
    private Long maxMsgAge;
    @Max(value = 8388608)
    @NotNull
    @JsonProperty("maxMsgSize")
    private Long maxMsgSize;
   
    @Valid
    @NotEmpty
    private List<@Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") String> topicIds;

    @JsonIgnore
    private ObjectId id;

    private Boolean encryption;

    @JsonIgnore
    public List<String> findArraySubjectName() {
        if (topicIds.isEmpty())
            return null;

        List<String> topics = new ArrayList<String>();
        topicIds.forEach(topic -> {
        	
        	topics.add(getStreamName().concat(".").concat(topic));
        });
        return topics;
    }
}
