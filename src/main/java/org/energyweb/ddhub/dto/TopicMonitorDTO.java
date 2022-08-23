package org.energyweb.ddhub.dto;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class TopicMonitorDTO {
	private String id;
	@NonNull
	private String owner;
    private Long lastTopicUpdate = TimeUnit.NANOSECONDS.toNanos(new Date().getTime());
	private Long lastTopicVersionUpdate = TimeUnit.NANOSECONDS.toNanos(new Date().getTime());
}
