package org.energyweb.ddhub.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "channel")
@Data
@NoArgsConstructor
public class Channel {
	private ObjectId id;
    @NotNull
    @Max(value = 1440)
    private Long maxMsgAge;
    @Max(value = 8388608)
    @NotNull
    private Long maxMsgSize;
    @Valid
    @NotEmpty
    private List<@Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") String> topicIds;
    private Boolean encryption;
    @NotNull
	@NotEmpty
	@Size(max = 200, message = "The maximum length is 200 characters")
	private String fqcn;

}
