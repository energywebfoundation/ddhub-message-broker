package org.energyweb.ddhub.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChannelDTO {

    private String fqcn;
    private String topic;
    private Long maxMsgAge;
    private Long maxMsgSize;
}
