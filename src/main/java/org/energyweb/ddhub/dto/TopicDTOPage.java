package org.energyweb.ddhub.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopicDTOPage {

	@JsonProperty(value = "total")
	private long count;
	private long limit;
	private int page;
	private List<TopicDTO> records;
}